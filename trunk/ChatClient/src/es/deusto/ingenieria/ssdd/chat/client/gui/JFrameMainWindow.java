package es.deusto.ingenieria.ssdd.chat.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import es.deusto.ingenieria.ssdd.chat.client.controller.ChatClientController;
import es.deusto.ingenieria.ssdd.chat.client.controller.MessageReceiverInterface;

public class JFrameMainWindow extends JFrame implements MessageReceiverInterface {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtFieldServerIP;
	private JTextField txtFieldServerPort;
	private JTextField txtFieldLocalPort;
	private JTextField txtFieldNick;
	private JButton btnConnect;
	private JList<String> listUsers;
	private JTextPane textAreaHistory;
	private JTextArea textAreaSendMsg;
	private JButton btnSendMsg;
	private SimpleDateFormat textFormatter = new SimpleDateFormat("HH:mm:ss");
	private JDialog waitingInvitationPane;
	private JDialog invitationPane;
	private boolean isProgrammatic = false;
	
	private ChatClientController controller;	

	/**
	 * Create the frame.
	 */
	public JFrameMainWindow(ChatClientController controller) {
		this.controller = controller;
		//Add the frame as Observer of the Controller
		this.controller.addLocalObserver(this);
		
		setResizable(false);
		setType(Type.UTILITY);
		setTitle("Chat main window");
		setBounds(100, 100, 800, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JPanel panelUsers = new JPanel();
		panelUsers.setBorder(new TitledBorder(null, "Connected users", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelUsers, BorderLayout.EAST);
		panelUsers.setLayout(new BorderLayout(0, 0));
		panelUsers.setPreferredSize(new Dimension(200, 0));
		panelUsers.setMinimumSize(new Dimension(200, 0));
		
		listUsers = new JList<>();
		listUsers.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		listUsers.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		listUsers.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent arg0) {
				selectUser();
			}			
		});
		
		panelUsers.add(listUsers);
		
		JPanel panelConnect = new JPanel();
		panelConnect.setBorder(new TitledBorder(null, "Connection details", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelConnect, BorderLayout.NORTH);
		
		JLabel lblServerIp = new JLabel("Server IP:");		
		JLabel lblServerPort = new JLabel("Server Port:");
		JLabel lblLocalPort = new JLabel("Local Port:");
		
		txtFieldServerIP = new JTextField();
		txtFieldServerIP.setColumns(10);
		txtFieldServerPort = new JTextField();
		txtFieldServerPort.setColumns(10);
		
		txtFieldLocalPort = new JTextField();
		txtFieldLocalPort.setColumns(10);
		
		JLabel lblNick = new JLabel("Nick:");
		
		txtFieldNick = new JTextField();
		txtFieldNick.setColumns(10);
		
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				btnConnectClick();
			}
		});
		btnConnect.setToolTipText("Connect");
		GroupLayout gl_panelConnect = new GroupLayout(panelConnect);
		gl_panelConnect.setHorizontalGroup(
			gl_panelConnect.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panelConnect.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panelConnect.createParallelGroup(Alignment.LEADING)
						.addComponent(lblServerIp)
						.addComponent(lblServerPort))
					.addGap(21)
					.addGroup(gl_panelConnect.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panelConnect.createSequentialGroup()
							.addComponent(txtFieldServerIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(37)
							.addComponent(lblNick)
							.addGap(18)
							.addComponent(txtFieldNick, GroupLayout.PREFERRED_SIZE, 115, GroupLayout.PREFERRED_SIZE)
							.addGap(28)
							.addComponent(btnConnect))
						.addGroup(gl_panelConnect.createSequentialGroup()
							.addComponent(txtFieldServerPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addGap(37)
							.addComponent(lblLocalPort)
							.addGap(18)
							.addComponent(txtFieldLocalPort, GroupLayout.PREFERRED_SIZE, 115, GroupLayout.PREFERRED_SIZE)))
//						.addComponent(txtFieldServerPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(302, Short.MAX_VALUE))
		);
		gl_panelConnect.setVerticalGroup(
			gl_panelConnect.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panelConnect.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panelConnect.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblServerIp)
						.addComponent(txtFieldServerIP, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblNick)
						.addComponent(btnConnect)
						.addComponent(txtFieldNick, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panelConnect.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblServerPort)
						.addComponent(txtFieldServerPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblLocalPort)
						.addComponent(txtFieldLocalPort, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap(15, Short.MAX_VALUE))
		);
		panelConnect.setLayout(gl_panelConnect);
		
		JPanel panelHistory = new JPanel();
		panelHistory.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "History", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelHistory, BorderLayout.CENTER);
		panelHistory.setLayout(new BorderLayout(0, 0));
		
		textAreaHistory = new JTextPane();
		textAreaHistory.setBackground(Color.BLACK);
		textAreaHistory.setToolTipText("Messages history");
		textAreaHistory.setEditable(false);
		
		JScrollPane scrollPaneHistory = new JScrollPane(textAreaHistory);
		scrollPaneHistory.setViewportBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelHistory.add(scrollPaneHistory);
		
		JPanel panelSendMsg = new JPanel();
		contentPane.add(panelSendMsg, BorderLayout.SOUTH);
		panelSendMsg.setBorder(new TitledBorder(null, "New message", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelSendMsg.setLayout(new BorderLayout(0, 0));
		
		btnSendMsg = new JButton("Send");
		btnSendMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				btnSendClick();
			}
		});
		btnSendMsg.setToolTipText("Send new message");
		btnSendMsg.setEnabled(false);
		panelSendMsg.add(btnSendMsg, BorderLayout.EAST);
		
		textAreaSendMsg = new JTextArea();
		textAreaSendMsg.setTabSize(3);
		textAreaSendMsg.setRows(4);
		textAreaSendMsg.setToolTipText("New message");	
		
		JScrollPane scrollPaneNewMsg = new JScrollPane(textAreaSendMsg);
		scrollPaneNewMsg.setViewportBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panelSendMsg.add(scrollPaneNewMsg, BorderLayout.CENTER);	
		
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (JFrameMainWindow.this.controller.isConnected()){
					disconnect();	
				}				
				super.windowClosing(e);
			}
		});
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private void btnConnectClick() {
		if (!this.controller.isConnected()) {
			if (this.txtFieldServerIP.getText().trim().isEmpty() ||
				this.txtFieldServerIP.getText().trim().isEmpty() ||
				this.txtFieldNick.getText().trim().isEmpty() ) {				
				JOptionPane.showMessageDialog(this, "Some connection parameters are empty", "Connection initializarion error", JOptionPane.ERROR_MESSAGE);				
				
				return;
			} else if (this.txtFieldNick.getText().contains(" ")) {
				JOptionPane.showMessageDialog(this, "The user nick cannot contain any spaces", "Nick error", JOptionPane.ERROR_MESSAGE);				
				
				return;
			}
			
			this.txtFieldServerIP.setEditable(false);
			this.txtFieldServerPort.setEditable(false);
			this.txtFieldLocalPort.setEditable(false);
			this.txtFieldNick.setEditable(false);
			this.btnConnect.setEnabled(false);
			
			//Connect to the server
			try {
				this.controller.connect(this.txtFieldServerIP.getText(),
				        Integer.parseInt(this.txtFieldServerPort.getText()),
				        Integer.parseInt(this.txtFieldLocalPort.getText()),
				        this.txtFieldNick.getText());
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Connection cannot be established. Try again later", "Error connecting", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			disconnect();
		}
	}
	
	private void disconnectedUI() {
		this.txtFieldServerIP.setEditable(true);
		this.txtFieldServerPort.setEditable(true);
		this.txtFieldLocalPort.setEditable(true);
		this.txtFieldNick.setEditable(true);
		this.listUsers.setEnabled(true);
		this.listUsers.clearSelection();
		this.listUsers.setModel(new DefaultListModel<String>());
		this.btnConnect.setEnabled(true);
		this.btnConnect.setText("Connect");
		this.btnSendMsg.setEnabled(false);
		this.textAreaHistory.setText("");
		this.textAreaSendMsg.setText("");
		
		this.setTitle("Chat main window - 'Disconnected'");
	}
	
	private void disconnect() {
		if (this.controller.isChatSessionOpened()) {
			try {
				this.controller.sendChatClosure();
			} catch (IOException e1) {
				e1.printStackTrace();						
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Disconnect from the server
//		if (this.controller.disconnect()) {
			this.controller.disconnect();
			disconnectedUI();
//		} else {
//			JOptionPane.showMessageDialog(this, "Disconnection from the server fails.", "Disconnection error", JOptionPane.ERROR_MESSAGE);				
//		}
	}
	
	private void selectUser() {
		if (isProgrammatic) {
			isProgrammatic = false;
			return;
		}
		if (this.listUsers.getSelectedIndex() != -1 && this.controller.getConnectedUser() != null) {		
			//Send chat Request
			if (!this.controller.isChatSessionOpened()) {			
				int result = JOptionPane.showConfirmDialog(this, "Do you want to start a new chat session with '" + this.listUsers.getSelectedValue() + "'", "Open chat Session", JOptionPane.YES_NO_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					final JOptionPane innerPane = new JOptionPane("Waiting invitation answer from "
							+ this.listUsers.getSelectedValue() + "...", JOptionPane.PLAIN_MESSAGE,
							JOptionPane.DEFAULT_OPTION, null, new String[]{"Cancel"});
					waitingInvitationPane = new JDialog(this, 
                            "Waiting answer",
                            true);
					waitingInvitationPane.setContentPane(innerPane);
					waitingInvitationPane.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					innerPane.addPropertyChangeListener(
					    new PropertyChangeListener() {
					        public void propertyChange(PropertyChangeEvent e) {
					            String prop = e.getPropertyName();

					            if (waitingInvitationPane.isVisible() 
					             && (e.getSource() == innerPane)
					             && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
					                waitingInvitationPane.setVisible(false);
					            }
					        }
					    });
					
					try {
						waitingInvitationPane.pack();
						this.controller.sendChatRequest(this.listUsers.getSelectedValue());
						waitingInvitationPane.setVisible(true);
					} catch (IOException e1) {
						e1.printStackTrace();
						waitingInvitationPane.setVisible(false);
						JOptionPane.showMessageDialog(this, "Chat cannot be started. Try again later", "Error sending chat request", JOptionPane.ERROR_MESSAGE);
					}
					
					String value = (String) innerPane.getValue();
					if (value != null && value.equals("Cancel")) {
						try {
							controller.cancelInvitation(listUsers.getSelectedValue());
						} catch (IOException e) {
							e.printStackTrace();
							//JOptionPane.showMessageDialog(this, "Chat cannot be started. Try again later", "Error sending chat request", JOptionPane.ERROR_MESSAGE);
						}
					}
				} else {
					this.listUsers.clearSelection();
				}
			//Send a chat closure
			} else if (this.controller.isChatSessionOpened() && this.listUsers.getSelectedValue().equals(this.controller.getChatReceiver())) {			
				int result = JOptionPane.showConfirmDialog(this, "Do you want to close your current chat session with '" + this.controller.getChatReceiver() + "'", "Close chat Session", JOptionPane.YES_NO_OPTION);				

				if (result == JOptionPane.OK_OPTION) {
					try {
						this.controller.sendChatClosure();
					} catch (IOException e) {
						e.printStackTrace();						
					}
					this.listUsers.clearSelection();					
					this.setTitle("Chat main window - 'Connected'");
				}
			}
		}
	}
	
	private void btnSendClick() {
		if (!this.textAreaSendMsg.getText().trim().isEmpty()) {
		
			if (this.listUsers.getSelectedIndex() == -1) {				
				JOptionPane.showMessageDialog(this, "You haven't select a destination user", "Chat initialization error", JOptionPane.ERROR_MESSAGE);				
				
				return;
			}			
			
			String message = this.textAreaSendMsg.getText().trim();
			try {
				this.controller.sendMessage(message);
				this.appendSentMessageToHistory();
				this.textAreaSendMsg.setText("");
			} catch(IOException ex) {
				JOptionPane.showMessageDialog(this, "Message can't be delivered. Try again later", "Error sending a message", JOptionPane.ERROR_MESSAGE);				
			}
		}
	}
	
	private void appendSentMessageToHistory() {		
		String time = textFormatter.format(GregorianCalendar.getInstance().getTime());		
		String newMessage = " " + time + " - [" + this.controller.getConnectedUser() + "]: " + this.textAreaSendMsg.getText() + "\n";
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setBold(attrs, true);
		StyleConstants.setForeground(attrs, Color.WHITE);
		
		try {
			this.textAreaHistory.getStyledDocument().insertString(this.textAreaHistory.getStyledDocument().getLength(), newMessage, attrs);
		} catch (BadLocationException e) {
			System.err.println("# Error updating message history: " + e.getMessage());
		} 
	}
	
	private void appendReceivedMessageToHistory(String message, String user, long timestamp) {		
		String time = textFormatter.format(new Date(timestamp));		
		String newMessage = " " + time + " - [" + user + "]: " + message.trim() + "\n";
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setBold(attrs, true);
		StyleConstants.setForeground(attrs, Color.MAGENTA);		
		
		try {
			this.textAreaHistory.getStyledDocument().insertString(this.textAreaHistory.getStyledDocument().getLength(), newMessage, attrs);
		} catch (BadLocationException e) {
			System.err.println("# Error updating message history: " + e.getMessage());
		} 
	}

	@Override
	public void onMessageReceived(String message, String user) {
		appendReceivedMessageToHistory(message, user, System.currentTimeMillis());
	}

	@Override
	public void onConnect(boolean connected) {
		if (connected) {
			this.btnConnect.setEnabled(true);
			this.btnConnect.setText("Disconnect");
			this.btnSendMsg.setEnabled(true);
			this.textAreaSendMsg.setText("");
			
			this.setTitle("Chat main window - 'Connected'");
		} else {
			JOptionPane.showMessageDialog(this, "Can't connect to the server.", "Connection error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void onUsersUpdated(List<String> users) {
		DefaultListModel<String> listModel = new DefaultListModel<>();
		
		for (String user : users) {
			listModel.addElement(user);
		}
		
		this.listUsers.setModel(listModel);
	}

	@Override
	public void onError(String error) {
		// TODO Show errors
		if (error.equals("RESTART")) {
//			disconnect();
//			JOptionPane.showMessageDialog(this, "An error occurred in the server. Please connect again", "Critical error", JOptionPane.ERROR_MESSAGE);
			if (this.controller.isConnected()) {
				if (this.controller.isChatSessionOpened()) {
					onChatDisconnect(this.controller.getChatReceiver());
				} else if (waitingInvitationPane != null) {
					JOptionPane.showMessageDialog(this, "Invitation cannot be delivered. Try again later", "Invitation error", JOptionPane.INFORMATION_MESSAGE);
					onConnect(true);
				}
			} else {
				disconnectedUI();
			}
		} else if(error.equals("ERROR nick")){
			disconnectedUI();
			JOptionPane.showMessageDialog(this, "Nick in use. Please introduce another one", "Nick error", JOptionPane.ERROR_MESSAGE);
		}else if (error.equals("ERROR user")){
			JOptionPane.showMessageDialog(this, "User is not connected. Please select another user", "User error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void onChatRequestResponse(String user, boolean accept) {
		if (accept) {
			if (waitingInvitationPane != null) {
				waitingInvitationPane.setVisible(false);
				waitingInvitationPane = null;
			}
			this.setTitle("Chat session between '" + this.controller.getConnectedUser() + "' & '" + user + "'");
		} else {
			if (waitingInvitationPane != null) {
				waitingInvitationPane.setVisible(false);
				waitingInvitationPane = null;
			}
			this.listUsers.clearSelection();
			JOptionPane.showMessageDialog(this, user + " refused the invitation to start a chat", "Invitation refused", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	@Override
	public void onChatDisconnect(String user) {
		JOptionPane.showMessageDialog(this, user + " closed the chat session", "Chat closed", JOptionPane.WARNING_MESSAGE);
		this.listUsers.clearSelection();					
		this.setTitle("Chat main window - 'Connected'");
	}

//	@Override
//	public void onChatInvitationReceived(String user) {
//		int op = JOptionPane.showConfirmDialog(this, "Would you like to start a chat session with " + user + "?", "Start new chat session", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
//		try {
//			if (op == JOptionPane.CLOSED_OPTION || op == JOptionPane.NO_OPTION) {
//				this.controller.refuseChatRequest(user);
//			} else {
//				this.controller.acceptChatRequest(user);
//				this.setTitle("Chat session between '" + this.controller.getConnectedUser() + "' & '" + user + "'");
//			}
//		} catch(IOException ex) {
//			ex.printStackTrace();
//			JOptionPane.showMessageDialog(this, "Error connecting to the server. Try again later", "Connection error", JOptionPane.ERROR_MESSAGE);
//		}
//	}

	@Override
	public void onInvitationCancelled(String user) {
//		if (waitingInvitationPane != null) {
//			waitingInvitationPane.setVisible(false);
//			waitingInvitationPane = null;
//			JOptionPane.showMessageDialog(this, user + " cancelled the invitation to start a chat", "Invitation cancelled", JOptionPane.INFORMATION_MESSAGE);
//		}
		if (invitationPane != null) {
			invitationPane.setVisible(false);
			invitationPane = null;
			JOptionPane.showMessageDialog(this, user + " cancelled the invitation to start a chat", "Invitation cancelled", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	@Override
	public void onChatInvitationReceived(String user) {
		final JOptionPane innerPane = new JOptionPane("Would you like to start a chat session with "
				+ user + "?", JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION);
		invitationPane = new JDialog(this, 
				"Start new chat session",
                true);
		invitationPane.setContentPane(innerPane);
		invitationPane.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		innerPane.addPropertyChangeListener(
		    new PropertyChangeListener() {
		        public void propertyChange(PropertyChangeEvent e) {
		            String prop = e.getPropertyName();

		            if (invitationPane.isVisible() 
		             && (e.getSource() == innerPane)
		             && (prop.equals(JOptionPane.VALUE_PROPERTY))) {
		            	invitationPane.setVisible(false);
		            }
		        }
		    });
		
		invitationPane.pack();
		invitationPane.setVisible(true);
		
		int value = ((Integer)innerPane.getValue()).intValue();
		try {
			if (value == JOptionPane.YES_OPTION) {
				this.controller.acceptChatRequest(user);
				isProgrammatic = true;
				this.listUsers.setSelectedValue(user, true);
				this.setTitle("Chat session between '" + this.controller.getConnectedUser() + "' & '" + user + "'");
			} else if (value == JOptionPane.NO_OPTION) {
				this.controller.refuseChatRequest(user);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error connecting to the server. Try again later", "Connection error", JOptionPane.ERROR_MESSAGE);
		}
	}

//	@Override
//	public void update(Observable observable, Object object) {
//		
//		//Update this method to process the request received from other users
//		
//		if (this.controller.isConnected()) {			
//			if (object.getClass().getName().equals(Message.class.getName())) {
//				Message newMessage = (Message) object;
//			
//				if (newMessage.getTo().getNick() == this.controller.getConnectedUser()) {
//					this.appendReceivedMessageToHistory(newMessage.getText(), newMessage.getFrom().getNick(), newMessage.getTimestamp());
//				}
//			}
//		}
//	}	
}