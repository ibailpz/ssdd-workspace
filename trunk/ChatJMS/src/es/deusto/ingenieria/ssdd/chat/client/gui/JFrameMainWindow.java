package es.deusto.ingenieria.ssdd.chat.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import es.deusto.ingenieria.ssdd.chat.client.controller.ChatClientController;
import es.deusto.ingenieria.ssdd.chat.client.controller.MessageReceiverInterface;

public class JFrameMainWindow extends JFrame implements
		MessageReceiverInterface {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
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
	private boolean isUIConnected = false;
	private DefaultListModel<String> listModel;

	private ChatClientController controller;

	/**
	 * Create the frame.
	 */
	public JFrameMainWindow(ChatClientController controller) {
		this.controller = controller;
		// Add the frame as Observer of the Controller
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
		panelUsers.setBorder(new TitledBorder(null, "Connected users",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelUsers, BorderLayout.EAST);
		panelUsers.setLayout(new BorderLayout(0, 0));
		panelUsers.setPreferredSize(new Dimension(200, 0));
		panelUsers.setMinimumSize(new Dimension(200, 0));

		listUsers = new JList<>();
		listUsers
				.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		listUsers.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));

		listModel = new DefaultListModel<>();
		listUsers.setModel(listModel);
		listUsers.addMouseListener(new MouseAdapter() {

			public void mousePressed(MouseEvent evt) {
				selectUser();
			}
		});

		panelUsers.add(listUsers);

		JPanel panelConnect = new JPanel();
		panelConnect.setBorder(new TitledBorder(null, "Connection details",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelConnect, BorderLayout.NORTH);

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
		gl_panelConnect
				.setHorizontalGroup(gl_panelConnect
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panelConnect
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												gl_panelConnect
														.createParallelGroup(
																Alignment.LEADING)
														.addGroup(
																gl_panelConnect
																		.createSequentialGroup()
																		.addGap(37)
																		.addComponent(
																				lblNick)
																		.addGap(18)
																		.addComponent(
																				txtFieldNick,
																				GroupLayout.PREFERRED_SIZE,
																				115,
																				GroupLayout.PREFERRED_SIZE)
																		.addGap(28)
																		.addComponent(
																				btnConnect)))));
		gl_panelConnect
				.setVerticalGroup(gl_panelConnect
						.createParallelGroup(Alignment.LEADING)
						.addGroup(
								gl_panelConnect
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												gl_panelConnect
														.createParallelGroup(
																Alignment.BASELINE)
														.addComponent(lblNick)
														.addComponent(
																btnConnect)
														.addComponent(
																txtFieldNick,
																GroupLayout.PREFERRED_SIZE,
																GroupLayout.DEFAULT_SIZE,
																GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												ComponentPlacement.RELATED)
										.addContainerGap(15, Short.MAX_VALUE)));
		panelConnect.setLayout(gl_panelConnect);

		JPanel panelHistory = new JPanel();
		panelHistory.setBorder(new TitledBorder(UIManager
				.getBorder("TitledBorder.border"), "History",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		contentPane.add(panelHistory, BorderLayout.CENTER);
		panelHistory.setLayout(new BorderLayout(0, 0));

		textAreaHistory = new JTextPane();
		textAreaHistory.setBackground(Color.BLACK);
		textAreaHistory.setToolTipText("Messages history");
		textAreaHistory.setEditable(false);

		JScrollPane scrollPaneHistory = new JScrollPane(textAreaHistory);
		scrollPaneHistory.setViewportBorder(new EtchedBorder(
				EtchedBorder.LOWERED, null, null));
		panelHistory.add(scrollPaneHistory);

		JPanel panelSendMsg = new JPanel();
		contentPane.add(panelSendMsg, BorderLayout.SOUTH);
		panelSendMsg.setBorder(new TitledBorder(null, "New message",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
		scrollPaneNewMsg.setViewportBorder(new EtchedBorder(
				EtchedBorder.LOWERED, null, null));
		panelSendMsg.add(scrollPaneNewMsg, BorderLayout.CENTER);

		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (JFrameMainWindow.this.controller.isConnected()) {
					disconnect();
				}
				super.windowClosing(e);
			}
		});
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void btnConnectClick() {
		if (!this.controller.isConnected()) {
			if (this.txtFieldNick.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(this,
						"Some connection parameters are empty",
						"Connection initializarion error",
						JOptionPane.ERROR_MESSAGE);

				return;
			} else if (this.txtFieldNick.getText().contains(" ")) {
				JOptionPane.showMessageDialog(this,
						"The user nick cannot contain any spaces",
						"Nick error", JOptionPane.ERROR_MESSAGE);

				return;
			}
			this.txtFieldNick.setEditable(false);
			this.btnConnect.setEnabled(false);

			// Connect to the server
			try {
				this.controller.connect(this.txtFieldNick.getText());
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this,
						"Connection cannot be established. Try again later",
						"Error connecting", JOptionPane.ERROR_MESSAGE);
			}
		} else {
			disconnect();
		}
	}

	private void disconnectedUI() {
		this.txtFieldNick.setEditable(true);
		this.listUsers.setEnabled(true);
		this.listUsers.clearSelection();
		this.listModel.clear();
		this.btnConnect.setEnabled(true);
		this.btnConnect.setText("Connect");
		this.btnSendMsg.setEnabled(false);
		this.textAreaHistory.setText("");
		this.textAreaSendMsg.setText("");

		this.setTitle("Chat main window - 'Disconnected'");
		isUIConnected = false;
	}

	private void disconnect() {
		if (this.controller.isChatSessionOpened()) {
			try {
				this.controller.sendChatClosure();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.controller.disconnect();
		disconnectedUI();
	}

	private void selectUser() {
		System.out.println("Select user");
		if (isProgrammatic) {
			isProgrammatic = false;
			return;
		}
		if (this.listUsers.getSelectedIndex() != -1
				&& this.controller.getConnectedUser() != null) {
			System.out.println("Select user - if 1");
			// Send chat Request
			if (!this.controller.isChatSessionOpened()) {
				System.out.println("Select user - if 2");
				int result = JOptionPane.showConfirmDialog(this,
						"Do you want to start a new chat session with '"
								+ this.listUsers.getSelectedValue() + "'",
						"Open chat Session", JOptionPane.YES_NO_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					final JOptionPane innerPane = new JOptionPane(
							"Waiting invitation answer from "
									+ this.listUsers.getSelectedValue() + "...",
							JOptionPane.PLAIN_MESSAGE,
							JOptionPane.DEFAULT_OPTION, null,
							new String[] { "Cancel" });
					waitingInvitationPane = new JDialog(this, "Waiting answer",
							true);
					waitingInvitationPane.setContentPane(innerPane);
					waitingInvitationPane
							.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
					innerPane
							.addPropertyChangeListener(new PropertyChangeListener() {
								public void propertyChange(PropertyChangeEvent e) {
									String prop = e.getPropertyName();

									if (waitingInvitationPane.isVisible()
											&& (e.getSource() == innerPane)
											&& (prop.equals(JOptionPane.VALUE_PROPERTY))) {
										waitingInvitationPane.setVisible(false);
									}
								}
							});
					waitingInvitationPane.setResizable(false);

					try {
						waitingInvitationPane.pack();
						waitingInvitationPane.setLocationRelativeTo(null);
						this.controller.sendChatRequest(this.listUsers
								.getSelectedValue());
						waitingInvitationPane.setVisible(true);
					} catch (Exception e1) {
						e1.printStackTrace();
						waitingInvitationPane.setVisible(false);
						JOptionPane.showMessageDialog(this,
								"Chat cannot be started. Try again later",
								"Error sending chat request",
								JOptionPane.ERROR_MESSAGE);
					}

					String value = (String) innerPane.getValue();
					if (value != null && value.equals("Cancel")) {
						try {
							controller.cancelInvitation(listUsers
									.getSelectedValue());
							listUsers.clearSelection();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} else {
					this.listUsers.clearSelection();
				}
				// Send a chat closure
			} else if (this.controller.isChatSessionOpened()
					&& this.listUsers.getSelectedValue().equals(
							this.controller.getChatReceiver())) {
				System.out.println("Select user - if 3");
				int result = JOptionPane.showConfirmDialog(this,
						"Do you want to close your current chat session with '"
								+ this.controller.getChatReceiver() + "'",
						"Close chat Session", JOptionPane.YES_NO_OPTION);

				if (result == JOptionPane.OK_OPTION) {
					try {
						this.controller.sendChatClosure();
					} catch (Exception e) {
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
				JOptionPane.showMessageDialog(this,
						"You haven't select a destination user",
						"Chat initialization error", JOptionPane.ERROR_MESSAGE);

				return;
			}

			String message = this.textAreaSendMsg.getText().trim();
			try {
				this.controller.sendMessage(message);
				this.appendSentMessageToHistory();
				this.textAreaSendMsg.setText("");
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this,
						"Message can't be delivered. Try again later",
						"Error sending a message", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void appendSentMessageToHistory() {
		String time = textFormatter.format(GregorianCalendar.getInstance()
				.getTime());
		String newMessage = " " + time + " - ["
				+ this.controller.getConnectedUser() + "]: "
				+ this.textAreaSendMsg.getText() + "\n";
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setBold(attrs, true);
		StyleConstants.setForeground(attrs, Color.WHITE);

		try {
			this.textAreaHistory.getStyledDocument().insertString(
					this.textAreaHistory.getStyledDocument().getLength(),
					newMessage, attrs);
		} catch (BadLocationException e) {
			System.err.println("# Error updating message history: "
					+ e.getMessage());
		}
	}

	private void appendReceivedMessageToHistory(String message, String user,
			long timestamp) {
		String time = textFormatter.format(new Date(timestamp));
		String newMessage = " " + time + " - [" + user + "]: " + message.trim()
				+ "\n";
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		StyleConstants.setBold(attrs, true);
		StyleConstants.setForeground(attrs, Color.MAGENTA);

		try {
			this.textAreaHistory.getStyledDocument().insertString(
					this.textAreaHistory.getStyledDocument().getLength(),
					newMessage, attrs);
		} catch (BadLocationException e) {
			System.err.println("# Error updating message history: "
					+ e.getMessage());
		}
	}

	@Override
	public void onMessage(String message, String userFrom, String userTo) {
		appendReceivedMessageToHistory(message, userFrom,
				System.currentTimeMillis());
	}

	@Override
	public void onConnect(boolean connected) {
		if (connected) {
			if (isUIConnected) {
				return;
			}
			isUIConnected = true;
			this.btnConnect.setEnabled(true);
			this.btnConnect.setText("Disconnect");
			this.btnSendMsg.setEnabled(true);
			this.textAreaSendMsg.setText("");

			this.setTitle("Chat main window - 'Connected'");
		} else {
			JOptionPane.showMessageDialog(this, "Can't connect to the server.",
					"Connection error", JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public void onUserConnected(String user) {
		if (!listModel.contains(user)) {
			listModel.addElement(user);
		}
	}

	public void onUserDisconnected(String user) {
		listModel.removeElement(user);
	}

	@Override
	public void onError(String error) {
		if (error.equals("ERROR nick")) {
			if (waitingInvitationPane != null) {
				waitingInvitationPane.setVisible(false);
				waitingInvitationPane = null;
			}
			if (invitationPane != null) {
				invitationPane.setVisible(false);
				invitationPane = null;
			}

			disconnectedUI();

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					JOptionPane.showMessageDialog(JFrameMainWindow.this,
							"Nick in use. Please introduce another one",
							"Nick error", JOptionPane.ERROR_MESSAGE);
				}
			});
		}
	}

	@Override
	public void onChatRequestResponse(final String userFrom, String userTo,
			boolean accept) {
		if (accept) {
			if (waitingInvitationPane != null) {
				waitingInvitationPane.setVisible(false);
				waitingInvitationPane = null;
			}
			this.setTitle("Chat session between '"
					+ this.controller.getConnectedUser() + "' & '" + userFrom
					+ "'");
		} else {
			if (waitingInvitationPane != null) {
				waitingInvitationPane.setVisible(false);
				waitingInvitationPane = null;
			}
			this.listUsers.clearSelection();

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					JOptionPane
							.showMessageDialog(
									JFrameMainWindow.this,
									userFrom
											+ " refused the invitation to start a chat",
									"Invitation refused",
									JOptionPane.INFORMATION_MESSAGE);
				}
			});
		}
	}

	@Override
	public void onChatDisconnect(final String user) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JOptionPane.showMessageDialog(JFrameMainWindow.this, user
						+ " closed the chat session", "Chat closed",
						JOptionPane.WARNING_MESSAGE);
			}
		});
		this.listUsers.clearSelection();
		this.setTitle("Chat main window - 'Connected'");
	}

	@Override
	public void onInvitationCancelled(final String userFrom, String userTo) {
		if (invitationPane != null) {
			invitationPane.setVisible(false);
			invitationPane = null;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					JOptionPane
							.showMessageDialog(
									JFrameMainWindow.this,
									userFrom
											+ " cancelled the invitation to start a chat",
									"Invitation cancelled",
									JOptionPane.INFORMATION_MESSAGE);
				}
			});
		}
	}

	@Override
	public void onChatInvitation(final String userFrom) {
		final JOptionPane innerPane = new JOptionPane(
				"Would you like to start a chat session with " + userFrom + "?",
				JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION);
		invitationPane = new JDialog(this, "Start new chat session", true);
		invitationPane.setContentPane(innerPane);
		invitationPane.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		innerPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				String prop = e.getPropertyName();

				if (invitationPane.isVisible() && (e.getSource() == innerPane)
						&& (prop.equals(JOptionPane.VALUE_PROPERTY))) {
					invitationPane.setVisible(false);
				}
			}
		});

		invitationPane.pack();
		invitationPane.setResizable(false);
		invitationPane.setLocationRelativeTo(null);
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				invitationPane.setVisible(true);

				try {
					int value = Integer.parseInt(innerPane.getValue()
							.toString());
					if (value == JOptionPane.YES_OPTION) {
						controller.acceptChatRequest(userFrom);
						isProgrammatic = true;
						listUsers.setSelectedValue(userFrom, true);
						setTitle("Chat session between '"
								+ controller.getConnectedUser() + "' & '"
								+ userFrom + "'");
					} else if (value == JOptionPane.NO_OPTION) {
						controller.refuseChatRequest(userFrom);
					}
				} catch (NumberFormatException ex) {
					System.err.println("Handled error");
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(JFrameMainWindow.this,
							"Error connecting to the server. Try again later",
							"Connection error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}
}