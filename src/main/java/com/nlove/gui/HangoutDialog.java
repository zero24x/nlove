package com.nlove.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.nlove.handler.HangoutCommandHandler;

public class HangoutDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private HangoutCommandHandler hangoutCommandHandler;
    private JTextArea textAreaMsgs;
    private JTextArea textAreaMsg;
    private JButton btnSend;

    public HangoutDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                hangoutCommandHandler.stop();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                init();
            }
        });
        setResizable(false);

        setTitle("nlove");
        setIconImage(Toolkit.getDefaultToolkit().getImage(HangoutDialog.class.getResource("/main/resources/logo.png")));
        setBounds(100, 100, 592, 501);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(null);
        {
            JLabel lblNewLabel = new JLabel("Hangout - An open chat to make new acquaintances");
            lblNewLabel.setBounds(133, 11, 302, 14);
            lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
            contentPanel.add(lblNewLabel);
        }

        JPanel panelMsg = new JPanel();
        panelMsg.setBorder(new EmptyBorder(2, 2, 2, 2));
        panelMsg.setBounds(10, 40, 556, 106);
        contentPanel.add(panelMsg);
        panelMsg.setLayout(null);

        JLabel lblYourMessage = new JLabel("Your message (Up to 2 min msg confirm delay, do not write wrong things, we cannot delete a blockchain!)");
        lblYourMessage.setBounds(2, 2, 552, 15);
        panelMsg.add(lblYourMessage);

        textAreaMsg = new JTextArea();
        textAreaMsg.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (textAreaMsg.getText().length() >= 590) // limit to 3 characters
                    e.consume();
            }
        });
        textAreaMsg.setColumns(590);
        textAreaMsg.setFocusCycleRoot(true);
        textAreaMsg.setBounds(2, 22, 552, 55);
        textAreaMsg.setRows(6);
        textAreaMsg.setFont(new Font("Tahoma", Font.PLAIN, 11));
        panelMsg.add(textAreaMsg);

        btnSend = new JButton("Send");
        btnSend.setEnabled(false);
        btnSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        btnSend.setBounds(2, 82, 77, 22);
        btnSend.setIcon(new ImageIcon(HangoutDialog.class.getResource("/main/resources/envelope.png")));
        btnSend.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String text = textAreaMsg.getText().trim();

                if (text.length() > 0) {
                    setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    hangoutCommandHandler.sendMessage(text);
                    textAreaMsg.setText("");
                    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        btnSend.setActionCommand("OK");
        panelMsg.add(btnSend);

        JPanel panelHistory = new JPanel();
        panelHistory.setBorder(new EmptyBorder(2, 2, 2, 2));
        panelHistory.setBounds(10, 161, 556, 257);
        contentPanel.add(panelHistory);
        GridBagLayout gbl_panelHistory = new GridBagLayout();
        gbl_panelHistory.columnWidths = new int[] { 270, 270, 0 };
        gbl_panelHistory.rowHeights = new int[] { 20, 0, 0, 0 };
        gbl_panelHistory.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
        gbl_panelHistory.rowWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
        panelHistory.setLayout(gbl_panelHistory);

        JLabel lblMessages = new JLabel("Messages");
        GridBagConstraints gbc_lblMessages = new GridBagConstraints();
        gbc_lblMessages.fill = GridBagConstraints.BOTH;
        gbc_lblMessages.gridwidth = 2;
        gbc_lblMessages.insets = new Insets(0, 0, 5, 0);
        gbc_lblMessages.gridx = 0;
        gbc_lblMessages.gridy = 0;
        panelHistory.add(lblMessages, gbc_lblMessages);

        JScrollPane scrollPane = new JScrollPane();
        GridBagConstraints gbc_scrollPane = new GridBagConstraints();
        gbc_scrollPane.gridheight = 2;
        gbc_scrollPane.gridwidth = 2;
        gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
        gbc_scrollPane.fill = GridBagConstraints.BOTH;
        gbc_scrollPane.gridx = 0;
        gbc_scrollPane.gridy = 1;
        panelHistory.add(scrollPane, gbc_scrollPane);

        textAreaMsgs = new JTextArea();
        textAreaMsgs.setFont(new Font("Tahoma", Font.PLAIN, 11));
        textAreaMsgs.setEditable(false);
        scrollPane.setViewportView(textAreaMsgs);
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.LEFT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("Close");
                okButton.setIcon(new ImageIcon(HangoutDialog.class.getResource("/main/resources/times.png")));
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                    }
                });
                okButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        dispose();
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
        }

    }

    public void init() {
        textAreaMsgs.setText("Loading message history from IOTA network...");
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        EventQueue.invokeLater(new Runnable() {
            public void run() {

                textAreaMsg.setEnabled(false);
                btnSend.setEnabled(false);

                hangoutCommandHandler = new HangoutCommandHandler();
                hangoutCommandHandler.setTextAreaMsg(textAreaMsg);
                hangoutCommandHandler.setTextAreaMsgs(textAreaMsgs);
                try {
                    hangoutCommandHandler.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(contentPanel.getParent(), e.getMessage() + ", error:" + e.getCause(), "Error", JOptionPane.ERROR_MESSAGE);
                    dispose();
                }

                btnSend.setEnabled(true);
                textAreaMsg.setEnabled(true);
                textAreaMsgs.setEnabled(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    public JButton getBtnSend() {
        return btnSend;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
