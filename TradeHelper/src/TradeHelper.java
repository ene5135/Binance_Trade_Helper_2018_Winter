import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Dimension;
import javax.swing.JCheckBox;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.SwingConstants;

public class TradeHelper extends JFrame {

	private JPanel contentPane;
	private JTable table;
	static MyTableModel model = new MyTableModel();
	static AlramTableModel alramModel = new AlramTableModel();
	private JPanel panel;
	private JCheckBox checkBox1;
	private JCheckBox checkBox2;
	private JCheckBox checkBox3;
	private JCheckBox checkBox4;
	final static JLabel label = new JLabel("");
	
	static ArrayList<String> btcPairList;
	static Map<String, CoinInfo> coinInfoTable = new HashMap<String, CoinInfo>();
	static float m1Filter = (float) 0.0;
	static float m3Filter = (float) 0.0;
	static float m5Filter = (float) 0.0;
	static float m5VolumeFilter = (float) 0.0;
	static boolean m1Check = false;
	static boolean m3Check = false;
	static boolean m5Check = false;
	static boolean m5VolumeCheck = false;
	static boolean superPass = false;
	static int radioButtonValue = 1;
	
	static boolean serverState = true;
	
	static String alramSoundPath = "";
	
	static ImageIcon loadingImg = new ImageIcon(TradeHelper.class.getClassLoader().getResource("loader_gooey_liquid.gif"));
	static ImageIcon stopImg = new ImageIcon(TradeHelper.class.getClassLoader().getResource("warningSmall.png"));
	static ImageIcon iconImg = new ImageIcon(TradeHelper.class.getClassLoader().getResource("Line-chart-icon.png"));
	static AudioInputStream audioInputStream;
	static Clip clip;
	
    public static Session getWebsocketSession(String url, WebSocketAdapter adapter) throws Exception 
    {
        URI uri = new URI(url);
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true); // The magic
        WebSocketClient client = new WebSocketClient(sslContextFactory);
        client.start();
        return client.connect(adapter, uri).get();
    }
	
	/**
	 * Launch the application.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception 
	{  
		audioInputStream = AudioSystem.getAudioInputStream(TradeHelper.class.getClassLoader().getResource("alram_sound.wav"));
		clip = AudioSystem.getClip();
		clip.open(audioInputStream);
		
		btcPairList = getBtcPairs();
		
		Session h24Ticker = getWebsocketSession("wss://stream.binance.com:9443/ws/!ticker@arr", new WebSocketAdapter(){
			ArrayList<String> sessionBtcPairList = new ArrayList<String>(btcPairList);			
			@Override
            public void onWebSocketText (String message) 
            { 
				Gson gson = new Gson();  
        	    JsonArray ja = gson.fromJson(message, JsonElement.class).getAsJsonArray();
        	    
        	    for(int i = 0; i < ja.size();i++)
        	    {          
        	        JsonObject market = ja.get(i).getAsJsonObject();
        	        String symbol = market.get("s").getAsString();
        	        if(symbol.endsWith("BTC"))
        	        {
        	        	if(!sessionBtcPairList.contains(symbol))
        	        	{	
        	        		sessionBtcPairList.add(symbol);
        	        		btcPairList.add(symbol);
        	        		System.out.println("New coin is listed! : "+symbol);
        	        		EventQueue.invokeLater(new Runnable() {
        	        			public void run() {
        	        				clip.setFramePosition(0);
        	        				clip.start();
        	        				JOptionPane.showMessageDialog(null, "신규 코인 상장됨! : "+symbol, "신규 코인 상장 알림", JOptionPane.WARNING_MESSAGE);
        	        			}});		
        	        	}
        	        }
        	    }
            }
            
        });
		
		CoinInfo noCoin = new CoinInfo();
		noCoin.setName("noCoin");
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(new SubstanceGraphiteLookAndFeel());
					TradeHelper frame = new TradeHelper();	
					frame.setIconImage(iconImg.getImage());
					frame.setVisible(true);	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		while(true)
		{
			if(serverState)
			{
				ImageIcon ii = loadingImg;
				label.setIcon(ii);
			}
			else
			{
				ImageIcon ii = stopImg;
				label.setIcon(ii);
			}
			updateCoinInfoTable();
			model.deleteCoinInfo(noCoin);
			for(String cursor : btcPairList)
			{
				CoinInfo coinInfo = coinInfoTable.get(cursor);
				if(coinInfo == null)
				{
					continue;
				}
				if(isRecommendable(coinInfo))
				{
					model.addCoinInfo(coinInfo);
					System.out.println(cursor + " : "
					   + coinInfo.getM1PriceRate() + " " 
					   + coinInfo.getM3PriceRate() + " "
					   + coinInfo.getM5PriceRate() + " "
					   + coinInfo.getCombo() + " "
					   + coinInfo.getM5VolumeRate() + " "
					   + coinInfo.isM15PM() + " "
					   + coinInfo.isM30PM() + " "
					   + coinInfo.isM60PM());
				}
				else
				{
					model.deleteCoinInfo(coinInfo);
				}
			}
			if(model.getRowCount() == 0)
			{
				model.addCoinInfo(noCoin);
			}
       		EventQueue.invokeLater(new Runnable() {
    			public void run() {
    				model.fireTableDataChanged();    		
    				}});
			Thread.sleep(5500);
		}


	}

	/**
	 * Create the frame.
	 * @throws UnsupportedLookAndFeelException 
	 */
	public TradeHelper() throws UnsupportedLookAndFeelException 
	{

		setTitle("Trade Helper");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 500, 340);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		//setContentPane(contentPane);
		JTabbedPane jtab = new JTabbedPane();
		
		TableColumnModel columnModel = new DefaultTableColumnModel();
		String[] columnNames = {"코인", "1분", "3분", "5분", "콤보", "5분 거래량", "15분", "30분", "60분"};
		
		SubstanceDefaultTableCellRenderer cellRenderer = new SubstanceDefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
			{
				Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if(column == 1 || column == 2 || column == 3 || column == 5)
				{
					float cellValue;
					try {
						cellValue =  Float.parseFloat(value.toString());
					} catch (NumberFormatException e) {
						cellValue = 0;
					}
					if(cellValue >= 1 && column != 5)
					{
						cell.setForeground(Color.GREEN);
					}
					else if(cellValue > 0)
					{
						cell.setForeground(new Color(100,230,100));
					}
					else if(cellValue <= -1 && column != 5)
					{
						cell.setForeground(Color.RED);
					}
					else if(cellValue < 0)
					{
						cell.setForeground(new Color(255,80,90));
					}
				}
				else if(column == 4)
				{
					int comboValue;
					try {
						comboValue = Integer.parseInt(value.toString());
					} catch (NumberFormatException e) {
						comboValue = 0;
					}
					if(comboValue >= 5)
					{
						cell.setForeground(Color.GREEN);
					}
					else if(comboValue >= 2)
					{
						cell.setForeground(new Color(100,230,100));
					}

				}
				else if(column == 6 || column == 7 || column == 8)
				{
					cell.setFont(getFont().deriveFont((float) 15));
				}
				
				return this;
			}
			
		};
		cellRenderer.setHorizontalAlignment(JLabel.CENTER);

		
		for(int i=0; i<columnNames.length; i++)
		{
			TableColumn column = new TableColumn(i);
			column.setHeaderValue(columnNames[i]);
			column.setCellRenderer(cellRenderer);
			switch (i) {
	        case 0 :
	        	column.setMinWidth(55);
	        	break;
	        case 5 :
	        	column.setMinWidth(80);
	        	break;
	        default :
	        	column.setMinWidth(45);
	        }
			columnModel.addColumn(column);
		}
		contentPane.setLayout(new BorderLayout(0, 0));
		
		panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 10, 10, 0));
		contentPane.add(panel, BorderLayout.NORTH);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] {30, 168, 204, 0};
		gbl_panel.rowHeights = new int[] {24, 10, 0};
		gbl_panel.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
		gbl_panel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new EmptyBorder(0, 10, 0, 0));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.insets = new Insets(0, 0, 5, 5);
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 0;
		panel.add(panel_1, gbc_panel_1);
		panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		JTextField textField = new JTextField();
		
		textField.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m1Filter = Float.parseFloat(textField.getText());
			}
		});
		checkBox1 = new JCheckBox("1\uBD84 \uD544\uD130");
		checkBox1.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == e.SELECTED)
		        {
		        	m1Check = true;
		        	m1Filter = Float.parseFloat(textField.getText());
		        }
		        else
		        {
		        	m1Check = false;
		        }
		    }
		});

		panel_1.add(checkBox1);

		textField.setHorizontalAlignment(SwingConstants.TRAILING);
		textField.setText("0");
		textField.setPreferredSize(new Dimension(60, 20));
		panel_1.add(textField);
		JPanel panel_2 = new JPanel();
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.insets = new Insets(0, 0, 5, 5);
		gbc_panel_2.gridx = 2;
		gbc_panel_2.gridy = 0;
		panel.add(panel_2, gbc_panel_2);
		
		checkBox2 = new JCheckBox("3\uBD84 \uD544\uD130");
		JTextField textField2 = new JTextField();
		textField2.setHorizontalAlignment(SwingConstants.TRAILING);
		textField2.setText("0");
		textField2.setPreferredSize(new Dimension(60, 20));
		panel_2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel_2.add(checkBox2);
		panel_2.add(textField2);
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new EmptyBorder(0, 10, 0, 0));
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.insets = new Insets(0, 0, 0, 5);
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 1;
		panel.add(panel_3, gbc_panel_3);
		checkBox2.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == e.SELECTED)
		        {
		        	m3Check = true;
		        	m3Filter = Float.parseFloat(textField2.getText());
		        }
		        else
		        {
		        	m3Check = false;
		        }
		    }
		});
		textField2.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m3Filter = Float.parseFloat(textField2.getText());
			}
		});
		
		checkBox3 = new JCheckBox("5\uBD84 \uD544\uD130");
		JTextField textField3 = new JTextField();
		textField3.setHorizontalAlignment(SwingConstants.TRAILING);
		textField3.setText("0");
		textField3.setPreferredSize(new Dimension(60, 20));
		panel_3.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel_3.add(checkBox3);
		panel_3.add(textField3);
		JPanel panel_4 = new JPanel();
		panel_4.setBorder(new EmptyBorder(0, 0, 0, 0));
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.insets = new Insets(0, 0, 0, 5);
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 2;
		gbc_panel_4.gridy = 1;
		panel.add(panel_4, gbc_panel_4);
		checkBox3.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == e.SELECTED)
		        {
		        	m5Check = true;
		        	m5Filter = Float.parseFloat(textField3.getText());
		        }
		        else
		        {
		        	m5Check = false;
		        }
		    }
		});
		textField3.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m5Filter = Float.parseFloat(textField3.getText());
			}
		});
		
		
		checkBox4 = new JCheckBox("5\uBD84 \uAC70\uB798\uB7C9 \uD544\uD130");
		JTextField textField4 = new JTextField();
		textField4.setHorizontalAlignment(SwingConstants.TRAILING);
		textField4.setText("0");
		textField4.setPreferredSize(new Dimension(60, 20));
		panel_4.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
		panel_4.add(checkBox4);
		panel_4.add(textField4);
		checkBox4.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		        if (e.getStateChange() == e.SELECTED)
		        {
		        	m5VolumeCheck = true;
		        	m5VolumeFilter = Float.parseFloat(textField4.getText());
		        }
		        else
		        {
		        	m5VolumeCheck = false;
		        }
		    }
		});
		textField4.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				m5VolumeFilter = Float.parseFloat(textField4.getText());
			}
		});
		
		
		ImageIcon ii = loadingImg;

		
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.gridx = 3;
		gbc_label.gridy = 1;
		panel.add(label, gbc_label);
		label.setIcon(ii);
		
		jtab.addTab("홈 화면", contentPane);
		
		table = new JTable(model, columnModel);
		table.addMouseListener(new java.awt.event.MouseAdapter() {
		    @Override
		    public void mouseClicked(java.awt.event.MouseEvent evt) {
		        int row = table.convertRowIndexToModel(table.rowAtPoint(evt.getPoint()));
		        int col = table.columnAtPoint(evt.getPoint());
		        if (row >= 0 && col >= 0) {
		        	StringSelection stringSelection = new StringSelection((String) model.getValueAt(row, 0));
		        	Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		        	clpbrd.setContents(stringSelection, null);
		        }
		    }
		});
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);
		table.setShowVerticalLines(false);
		table.setRowHeight(23);
		table.setGridColor(Color.LIGHT_GRAY);
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		
		JScrollPane scrollPane = new JScrollPane(table);
		contentPane.add(scrollPane, BorderLayout.CENTER);
				
		JPanel alramPanel = new JPanel();
		jtab.addTab("알람 설정", alramPanel);
		alramPanel.setLayout(new GridLayout(0, 2, 0, 0));
		
		JPanel left = new JPanel();
		JPanel right = new JPanel();
		
		alramPanel.add(left);
		GridBagLayout gbl_left = new GridBagLayout();
		gbl_left.columnWidths = new int[]{79, 79, 73, 79, 0};
		gbl_left.rowHeights = new int[] {55, 38, 0, 0, 0, 40, 0};
		gbl_left.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_left.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		left.setLayout(gbl_left);
		
		JLabel lblNewLabel = new JLabel("\uC0C8 \uC54C\uB78C \uB4F1\uB85D");
		lblNewLabel.setFont(new Font("굴림", Font.PLAIN, 15));
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 2;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 1;
		left.add(lblNewLabel, gbc_lblNewLabel);
		
		JLabel lblNewLabel_1 = new JLabel("\uD398\uC5B4 \uC774\uB984");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 2;
		left.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		JTextField textField_1 = new JTextField();
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.fill = GridBagConstraints.BOTH;
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.gridx = 2;
		gbc_textField_1.gridy = 2;
		left.add(textField_1, gbc_textField_1);
		textField_1.setColumns(10);
		
		JLabel lblNewLabel_2 = new JLabel("\uC54C\uB78C \uAC00\uACA9");
		GridBagConstraints gbc_lblNewLabel_2 = new GridBagConstraints();
		gbc_lblNewLabel_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel_2.gridx = 1;
		gbc_lblNewLabel_2.gridy = 3;
		left.add(lblNewLabel_2, gbc_lblNewLabel_2);
		
		JTextField textField_2 = new JTextField();
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.fill = GridBagConstraints.BOTH;
		gbc_textField_2.insets = new Insets(0, 0, 5, 5);
		gbc_textField_2.gridx = 2;
		gbc_textField_2.gridy = 3;
		left.add(textField_2, gbc_textField_2);
		textField_2.setColumns(10);
		
		JButton btnNewButton = new JButton("\uB4F1\uB85D");
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.gridwidth = 2;
		gbc_btnNewButton.insets = new Insets(0, 0, 0, 5);
		gbc_btnNewButton.gridx = 1;
		gbc_btnNewButton.gridy = 5;
		left.add(btnNewButton, gbc_btnNewButton);
		btnNewButton.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent e) {
				 Alram newAlram = new Alram();
				 String newSymbol = textField_1.getText();
				 newAlram.setCoinName(newSymbol);
				 newAlram.setAlramPrice(Float.parseFloat(textField_2.getText()));
				 newAlram.setDirection(radioButtonValue);
				 alramModel.addAlram(newAlram);
				 WebSocketAdapter newAdapter = new WebSocketAdapter(){			
						Alram alram = newAlram;
						String symbol = newSymbol;
						float targetPrice = newAlram.getAlramPrice();
						int direction = newAlram.getDirection();
						@Override
					    public void onWebSocketText (String message) 
					    { 
							Gson gson = new Gson();  
							JsonObject jo = gson.fromJson(message, JsonElement.class).getAsJsonObject();
						    
					        JsonObject k = jo.get("k").getAsJsonObject();
					        float price = k.get("c").getAsFloat();
						    System.out.println(newSymbol + ": " + price);
						    if(direction == 1)
						    {
						        if(price >= targetPrice)
						        {
						        	alramModel.deleteRow(alramModel.data.indexOf(alram));
						        	EventQueue.invokeLater(new Runnable() {
					        			public void run() {
					        				clip.setFramePosition(0);
					        				clip.start();
					        				JOptionPane.showMessageDialog(null, newSymbol+" 코인의 가격이 "+targetPrice+" 에 상향 도달하였습니다.", "코인 가격 상향 도달 알림", JOptionPane.INFORMATION_MESSAGE);
					        			}});
						        	getSession().close();
						        }
						    }
						    else
						    {
						        if(price <= targetPrice)
						        {
						        	alramModel.deleteRow(alramModel.data.indexOf(alram));
						        	EventQueue.invokeLater(new Runnable() {
					        			public void run() {
					        				clip.setFramePosition(0);
					        				clip.start();
					        				JOptionPane.showMessageDialog(null, newSymbol+" 코인의 가격이 "+targetPrice+" 에 하향 도달하였습니다.", "코인 가격 하향 도달 알림", JOptionPane.INFORMATION_MESSAGE);
					        			}});
						        	getSession().close();
						        }
						    }
					    }
					    
					};
				  try {
						Session priceMonitor = getWebsocketSession("wss://stream.binance.com:9443/ws/"+newSymbol.toLowerCase()+"@kline_1m", newAdapter);
				  } catch (Exception e1) {
						// TODO Auto-generated catch block
					  e1.printStackTrace();
				  }
				  newAlram.setAdapter(newAdapter);
			 }
		});
		JRadioButton rdbtnNewRadioButton = new JRadioButton("\uC0C1\uD5A5 \uBAA9\uD45C");
		GridBagConstraints gbc_rdbtnNewRadioButton = new GridBagConstraints();
		gbc_rdbtnNewRadioButton.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNewRadioButton.gridx = 1;
		gbc_rdbtnNewRadioButton.gridy = 4;
		left.add(rdbtnNewRadioButton, gbc_rdbtnNewRadioButton);
		rdbtnNewRadioButton.setSelected(true);
		
		JRadioButton radioButton = new JRadioButton("\uD558\uD5A5 \uBAA9\uD45C");
		GridBagConstraints gbc_radioButton = new GridBagConstraints();
		gbc_radioButton.insets = new Insets(0, 0, 5, 5);
		gbc_radioButton.gridx = 2;
		gbc_radioButton.gridy = 4;
		left.add(radioButton, gbc_radioButton);
		
		rdbtnNewRadioButton.addItemListener(new SelectItemListener());
		radioButton.addItemListener(new SelectItemListener());
		
		ButtonGroup buttonGrp = new ButtonGroup();
        buttonGrp.add(rdbtnNewRadioButton);
        buttonGrp.add(radioButton);
 
		alramPanel.add(right);
		GridBagLayout gbl_right = new GridBagLayout();
		gbl_right.columnWidths = new int[]{207, 30, 0};
		gbl_right.rowHeights = new int[] {50, 155, 10, 30, 0};
		gbl_right.columnWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		gbl_right.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		right.setLayout(gbl_right);
		
		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 1;
		right.add(scrollPane_1, gbc_scrollPane_1);
		
		TableColumnModel alramColumnModel = new DefaultTableColumnModel();
		SubstanceDefaultTableCellRenderer alramCellRenderer = new SubstanceDefaultTableCellRenderer();
		alramCellRenderer.setHorizontalAlignment(JLabel.CENTER);
		TableColumn column0 = new TableColumn(0);	
		column0.setHeaderValue("페어 이름");
		column0.setCellRenderer(alramCellRenderer);
		column0.setMinWidth(45);
		alramColumnModel.addColumn(column0);
		TableColumn column1 = new TableColumn(1);	
		column1.setHeaderValue("알람 가격");
		column1.setCellRenderer(alramCellRenderer);
		column1.setMinWidth(45);
		alramColumnModel.addColumn(column1);
		TableColumn column2 = new TableColumn(2);	
		column2.setHeaderValue("방향");
		column2.setCellRenderer(alramCellRenderer);
		column2.setMinWidth(40);
		column2.setPreferredWidth(40);
		alramColumnModel.addColumn(column2);

		JTable table_1 = new JTable(alramModel, alramColumnModel);
		scrollPane_1.setViewportView(table_1);	
		
		table_1.setShowVerticalLines(false);
		table_1.setFocusable(false);
		table_1.setRowHeight(24);
		table_1.setGridColor(Color.LIGHT_GRAY);
		table_1.setAutoCreateRowSorter(true);
		table_1.getTableHeader().setReorderingAllowed(false);
		table_1.getTableHeader().setResizingAllowed(false);
		

		JButton btnNewButton_1 = new JButton("\uC54C\uB78C \uCDE8\uC18C");
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.insets = new Insets(0, 0, 5, 5);
		gbc_btnNewButton_1.gridx = 0;
		gbc_btnNewButton_1.gridy = 3;
		right.add(btnNewButton_1, gbc_btnNewButton_1);
		btnNewButton_1.addActionListener(new ActionListener() {
			 public void actionPerformed(ActionEvent e) {
				 if(table_1.getSelectedRow() != -1)
				 {
					 int row = table_1.getRowSorter().convertRowIndexToModel(table_1.getSelectedRow());
					 alramModel.data.get(row).getAdapter().getSession().close();
					 alramModel.deleteRow(row);
				 }
			 }
		});	
		
		getContentPane().add(jtab);
		//contentPane.add(scrollPane);
	}

	static class SelectItemListener implements ItemListener
    {
          public void itemStateChanged(ItemEvent e)
          {
                 AbstractButton sel = (AbstractButton)e.getItemSelectable();
                 if(e.getStateChange() == ItemEvent.SELECTED)
                 {
                        if (sel.getText().equals("상향 목표") )
                        {
                              radioButtonValue = 1;
                        }
                        else if (sel.getText().equals( "하향 목표" ) )
                        {
                        	  radioButtonValue = 0;
                        }
                 }
          }
    }
	
	static class CoinInfo
	{
		String name;
		float m1PriceRate;
		float m3PriceRate;
		float m5PriceRate;
		float m5VolumeRate;
		int combo;
		boolean m15PM;
		boolean m30PM;
		boolean m60PM;
		
		public CoinInfo()
		{
		}
		
		public CoinInfo(String symbol, float m1PriceRate2, float m3PriceRate2, float m5PriceRate2, float m5VolumeRate2,
				int combo2, boolean m15pm2, boolean m30pm2, boolean m60pm2) {
			name = symbol;
			m1PriceRate = m1PriceRate2;
			m3PriceRate = m3PriceRate2;
			m5PriceRate = m5PriceRate2;
			m5VolumeRate = m5VolumeRate2;
			combo = combo2;
			m15PM = m15pm2;
			m30PM = m30pm2;
			m60PM = m60pm2;
		}
		public float getM1PriceRate() {
			return m1PriceRate;
		}
		public float getM3PriceRate() {
			return m3PriceRate;
		}
		public float getM5PriceRate() {
			return m5PriceRate;
		}
		public float getM5VolumeRate() {
			return m5VolumeRate;
		}
		public int getCombo() {
			return combo;
		}
		public boolean isM15PM() {
			return m15PM;
		}
		public boolean isM30PM() {
			return m30PM;
		}
		public boolean isM60PM() {
			return m60PM;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public void setM1PriceRate(float m1PriceRate) {
			this.m1PriceRate = m1PriceRate;
		}
		public void setM3PriceRate(float m3PriceRate) {
			this.m3PriceRate = m3PriceRate;
		}
		public void setM5PriceRate(float m5PriceRate) {
			this.m5PriceRate = m5PriceRate;
		}
		public void setM5VolumeRate(float m5VolumeRate) {
			this.m5VolumeRate = m5VolumeRate;
		}
		public void setCombo(int combo) {
			this.combo = combo;
		}
		public void setM15PM(boolean m15pm) {
			m15PM = m15pm;
		}
		public void setM30PM(boolean m30pm) {
			m30PM = m30pm;
		}
		public void setM60PM(boolean m60pm) {
			m60PM = m60pm;
		}		
	}
	
	static class Alram {
		public String getCoinName() {
			return coinName;
		}
		public void setCoinName(String coinName) {
			this.coinName = coinName;
		}
		public float getAlramPrice() {
			return alramPrice;
		}
		public void setAlramPrice(float alramPrice) {
			this.alramPrice = alramPrice;
		}
		public WebSocketAdapter getAdapter() {
			return adapter;
		}
		public void setAdapter(WebSocketAdapter adapter) {
			this.adapter = adapter;
		}
		public int getDirection() {
			return direction;
		}
		public void setDirection(int direction) {
			this.direction = direction;
		}
		public WebSocketAdapter adapter;
		String coinName;
		float alramPrice;
		int direction;

	}
	
	static class AlramTableModel extends AbstractTableModel {
		String [] columnNames = {"페어 이름", "알람 가격", "방향"};
		ArrayList<Alram> data = new ArrayList<Alram>();
		
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}
		
		@Override
		public int getRowCount() {
			return data.size();
		}
		
		@Override
		public Class getColumnClass(int column) {
	        Class returnValue;
	        if ((column >= 0) && (column < getColumnCount())) {
	          returnValue = getValueAt(0, column).getClass();
	        } else {
	          returnValue = Object.class;
	        }
	        return returnValue;
		}
		
		@Override
		public Object getValueAt(int row, int col) {
			if(getRowCount() == 0)
			{
				return new Object();
			}
	        Alram alram = data.get(row);
	        if(col == 0)
	        {
	        	return alram.getCoinName();
	        }
	        else if(col == 1)
	        {
	        	return String.format("%.8f", alram.getAlramPrice()+0.0);
	        }
	        else
	        {
	        	if(alram.getDirection() == 1)
	        	{
	        		return "상향";
	        	}
	        	else
	        	{
	        		return "하향";
	        	}
	        }
		}
		
	    public void addAlram(Alram alram){
	        data.add(alram);
	        fireTableRowsInserted(data.size()-1, data.size()-1); // 반드시 호출해야한다.
	    }
	    
	    public void deleteRow(int row)
	    {
	    	data.remove(row);
	    	fireTableDataChanged();
	    }
	}
	
	static class MyTableModel extends AbstractTableModel {
		String[] columnNames = {"코인", "1분 상승률", "3분 상승률", "5분 상승률", "콤보", "5분 거래량 상승률", "15분 PM", "30분 PM", "60분 PM"};
		ArrayList<CoinInfo> data = new ArrayList<CoinInfo>();
		
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}
		
		@Override
		public Class getColumnClass(int column) {
	        Class returnValue;
	        if ((column >= 0) && (column < getColumnCount())) {
	          returnValue = getValueAt(0, column).getClass();
	        } else {
	          returnValue = Object.class;
	        }
	        return returnValue;
		}

		@Override
		public Object getValueAt(int row, int col) {
			if(getRowCount() == 0)
			{
				return new Object();
			}
	        CoinInfo info = data.get(row);
	        
	        if(info.getName().equals("noCoin"))
	        {
	        	switch (col) {
		        case 0 :
		            return "NaN";
		        case 1 :
		            return "NaN";
		        case 2 :
		            return "NaN";
		        case 3 :
		            return "NaN";
		        case 4 :
		            return "NaN";
		        case 5 :
		            return "NaN";
		        case 6 :
		            return "NaN";
		        case 7 :
		            return "NaN";
		        case 8 :
		            return "NaN";
		        default :
		                return "invalid";
		        }
	        }
	        
	        switch (col) {
	        case 0 :
	            return info.getName().substring(0, info.getName().length()-3);
	        case 1 :
	            return Float.parseFloat(String.format("%.2f", info.getM1PriceRate()+0.0));
	        case 2 :
	            return Float.parseFloat(String.format("%.2f", info.getM3PriceRate()+0.0));
	        case 3 :
	            return Float.parseFloat(String.format("%.2f", info.getM5PriceRate()+0.0));
	        case 4 :
	            return Integer.valueOf(info.getCombo()).toString();
	        case 5 :
	            return Float.parseFloat(String.format("%.2f", info.getM5VolumeRate()+0.0));
	        case 6 :
	            return TFtoPM(info.isM15PM());
	        case 7 :
	            return TFtoPM(info.isM30PM());
	        case 8 :
	            return TFtoPM(info.isM60PM());
	        default :
	                return "invalid";
	        }
	    }
		public String TFtoPM(boolean TF)
		{
			if(TF)
				return "+";
			else
				return "-";
		}
		
		public void updateCoinInfo(CoinInfo info)
		{
			for (CoinInfo cursor : data)
	    	{
	    		if(cursor.getName().equals(info.getName()))
	    		{
	    			data.set(data.indexOf(cursor), info);
	    			//fireTableRowsUpdated(data.indexOf(info), data.size()-1);
	    			return;
	    		}
	    	}
		}
		
	    public void addCoinInfo(CoinInfo info){
	    	for (CoinInfo cursor : data)
	    	{
	    		if(cursor.getName().equals(info.getName()))
	    		{
	    			updateCoinInfo(info);
	    			return;
	    		}
	    	}
	        data.add(info);
	        //fireTableRowsInserted(data.size()-1, data.size()-1); // 반드시 호출해야한다.
	    }
	    
	    public void deleteCoinInfo(CoinInfo info)
	    {
	    	for (CoinInfo cursor : data)
	    	{
	    		if(cursor.getName().equals(info.getName()))
	    		{
	    			int idx = data.indexOf(cursor);
	    			data.remove(idx);
	    			//fireTableRowsDeleted(idx, data.size()-1);
	    			return;
	    		}
	    	}
	    }

	}
	
	static boolean isRecommendable(CoinInfo coinInfo)
	{	
		return 	superPass || ((((coinInfo.getM1PriceRate() >= m1Filter) && m1Check) || !m1Check) 
				&& (((coinInfo.getM3PriceRate() >= m3Filter) && m3Check) || !m3Check)
				&& (((coinInfo.getM5PriceRate() >= m5Filter) && m5Check) || !m5Check)
				&& (((coinInfo.getM5VolumeRate() >= m5VolumeFilter) && m5VolumeCheck) || !m5VolumeCheck));
	}
	
	static ArrayList<String> getBtcPairs() throws Exception
	{
		String json_str = null;
		while(json_str == null)
		{
			json_str = getJsonStringFromUrl("https://api.binance.com/api/v1/ticker/24hr");  
		}
		Gson gson = new Gson();     
	    JsonArray ja = gson.fromJson(json_str, JsonElement.class).getAsJsonArray();

	    ArrayList<String> result = new ArrayList<String>();
	    
	    for(int i = 0; i < ja.size();i++)
	    {          
	        JsonObject market = ja.get(i).getAsJsonObject();
	        String symbol = market.get("symbol").getAsString();
	        if(symbol.endsWith("BTC"))
	        {
	        	result.add(symbol);
	        }
	    }
	
	    return result;
	}
	
	static String getJsonStringFromUrl(String urlString) throws Exception
	{
		try {
			URL url = new URL(urlString);
			
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setRequestMethod("GET");
			
			huc.connect();
			InputStream in = null;
			if( huc.getResponseCode() != 200 ){
			    in = huc.getErrorStream();
			    serverState = false;
			}else{
			    in = huc.getInputStream();
			    serverState = true;
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(in , "UTF-8"));
			String line = null;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
			    sb.append(line);
			}
			br.close();
			return sb.toString();
		} catch (ConnectException e) {
			return null;
		} 
	}
	
	static void updateCoinInfoTable() throws Exception
	{
		float m1PriceRate;
		float m3PriceRate;
		float m5PriceRate;
		float m5VolumeRate;
		int combo;
		boolean m15PM;
		boolean m30PM;
		boolean m60PM;
		
		for(String symbol : btcPairList)
		{
			String urlString = "https://api.binance.com/api/v1/klines?symbol=" + symbol + "&interval=1m&limit=60";
			String json_str = null;
			while(json_str == null)
			{
				json_str = getJsonStringFromUrl(urlString);
			}
	        Gson gson = new Gson();   
	        combo = 0;
	        
	        JsonArray m1candleArray = gson.fromJson(json_str, JsonElement.class).getAsJsonArray();
	       
	        JsonArray firstCandleInfo = m1candleArray.get(59).getAsJsonArray();
	        JsonArray thirdCandleInfo = m1candleArray.get(57).getAsJsonArray();
	        JsonArray fifthCandleInfo = m1candleArray.get(55).getAsJsonArray();
	        JsonArray fifteenthCandleInfo = m1candleArray.get(45).getAsJsonArray();
	        JsonArray thirtiethCandleInfo = m1candleArray.get(30).getAsJsonArray();
	        JsonArray sixtiethCandleInfo = m1candleArray.get(0).getAsJsonArray();
	        
	        //get(1) : open price, get(4) : close price, get(7) : btc volume
	        m1PriceRate = (firstCandleInfo.get(4).getAsFloat()-firstCandleInfo.get(1).getAsFloat()) * 100 / firstCandleInfo.get(1).getAsFloat();
	        m3PriceRate = (firstCandleInfo.get(4).getAsFloat()-thirdCandleInfo.get(1).getAsFloat()) * 100 / thirdCandleInfo.get(1).getAsFloat();
	        m5PriceRate = (firstCandleInfo.get(4).getAsFloat()-fifthCandleInfo.get(1).getAsFloat()) * 100 / fifthCandleInfo.get(1).getAsFloat();
	        m5VolumeRate = ((m1candleArray.get(59).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(58).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(57).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(56).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(55).getAsJsonArray().get(7).getAsFloat())
	        		-
	        		(m1candleArray.get(54).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(53).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(52).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(51).getAsJsonArray().get(7).getAsFloat()
	        		+ m1candleArray.get(50).getAsJsonArray().get(7).getAsFloat())) * 100
	        		/
	        		(m1candleArray.get(54).getAsJsonArray().get(7).getAsFloat()
	    	        + m1candleArray.get(53).getAsJsonArray().get(7).getAsFloat()
	    	        + m1candleArray.get(52).getAsJsonArray().get(7).getAsFloat()
	    	        + m1candleArray.get(51).getAsJsonArray().get(7).getAsFloat()
	    	        + m1candleArray.get(50).getAsJsonArray().get(7).getAsFloat());
	        for(int i=1;i<60;i++)
	        {
	        	if(m1candleArray.get(59-i).getAsJsonArray().get(4).getAsFloat()
	        			>= m1candleArray.get(59-i).getAsJsonArray().get(1).getAsFloat())
	        	{
	        		combo++;
	        	}
	        	else
	        	{
	        		break;
	        	}
	        }
	        m15PM = firstCandleInfo.get(4).getAsFloat() >= fifteenthCandleInfo.get(1).getAsFloat();
	        m30PM = firstCandleInfo.get(4).getAsFloat() >= thirtiethCandleInfo.get(1).getAsFloat();
	        m60PM = firstCandleInfo.get(4).getAsFloat() >= sixtiethCandleInfo.get(1).getAsFloat();
	        
	        coinInfoTable.put(symbol, new CoinInfo(symbol,m1PriceRate,m3PriceRate,m5PriceRate,m5VolumeRate,combo
	        		,m15PM,m30PM,m60PM));
		}
	}
}

//static public class candleWebSocketAdapter extends WebSocketAdapter {
//	
//	public String getSymbol() {
//		return symbol;
//	}
//	public float getM1PriceRate() {
//		return m1PriceRate;
//	}
//	public float getM3PriceRate() {
//		return m3PriceRate;
//	}
//	public float getM5PriceRate() {
//		return m5PriceRate;
//	}
//	public boolean isM15PM() {
//		return m15PM;
//	}
//	public boolean isM30PM() {
//		return m30PM;
//	}
//	public boolean isM60PM() {
//		return m60PM;
//	}
//	
//	String symbol;
//	float m1PriceRate;
//	float m3PriceRate;
//	float m5PriceRate;
//	boolean m15PM;
//	boolean m30PM;
//	boolean m60PM;
//	Gson gson = new Gson();
//	
//	
//	@Override
//	public void onWebSocketText (String message) 
//	{ 
//		JsonObject jo = gson.fromJson(message, JsonElement.class).getAsJsonObject();
//
//	    String stream = jo.get("stream").getAsString();
//	    JsonObject data = jo.get("data").getAsJsonObject();
//	    symbol = data.get("s").getAsString();
//        JsonObject k = data.get("k").getAsJsonObject();
//        float open = k.get("o").getAsFloat();
//        float close = k.get("c").getAsFloat();
//        float rate = (close-open)/open;
//        boolean PM = (rate >= 0);
//        
//	    if(stream.endsWith("1m"))
//	    {
//	    	m1PriceRate = rate;
//	    }
//	    else if(stream.endsWith("3m"))
//	    {
//	    	m3PriceRate = rate;
//	    }
//	    else if(stream.endsWith("5m"))
//	    {
//	    	m5PriceRate = rate;
//	    }
//	    else if(stream.endsWith("15m"))
//	    {
//	    	m15PM = PM;
//	    }
//	    else if(stream.endsWith("30m"))
//	    {
//	    	m30PM = PM;
//	    }
//	    else if(stream.endsWith("1h"))
//	    {
//	    	m60PM = PM;
//	    }    
//	    //System.out.println(symbol+" : "+rate);
//	}
//}
//
//public static ArrayList<candleWebSocketAdapter> openCoinTickers() throws Exception
//{
//	ArrayList<candleWebSocketAdapter> result = new ArrayList<candleWebSocketAdapter>();
//	for(String cursor : btcPairList)
//	{
//		String symbol = cursor.toLowerCase();
//		candleWebSocketAdapter newAdapter = new candleWebSocketAdapter();
//		Session candles = getWebsocketSession("wss://stream.binance.com:9443/stream?streams="+symbol+"@kline_1m/"
//				 +symbol+"@kline_3m/"
//				 +symbol+"@kline_5m/"
//				 +symbol+"@kline_15m/"
//				 +symbol+"@kline_30m/"
//				 +symbol+"@kline_1h",newAdapter);
//		result.add(newAdapter);
//	}
//	return result;
//}
