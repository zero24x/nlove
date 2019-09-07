package com.nlove.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.nlove.config.NloveProfile;

public class ShowMatchDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JTextField textFieldUsername;
    private JTextField textFieldYob;
    private JTextField textFieldGender;
    private JTextArea textAreaAbout;

    /**
     * Create the dialog.
     * 
     * @param owner
     * @param modal
     */
    public ShowMatchDialog(NloveProfile profile, Frame owner, boolean modal) {
        super(owner, modal);

        setTitle("nlove");
        setIconImage(Toolkit.getDefaultToolkit().getImage(ShowMatchDialog.class.getResource("/resources/logo.png")));
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(null);
        {
            JLabel lblNewLabel = new JLabel("Congrats, you rolled and found someone!");
            lblNewLabel.setBounds(103, 8, 236, 14);
            lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 11));
            contentPanel.add(lblNewLabel);
        }

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.setBounds(10, 33, 414, 166);
        contentPanel.add(panel);
        GridBagLayout gbl_panel = new GridBagLayout();
        gbl_panel.columnWidths = new int[] { 270, 270, 0 };
        gbl_panel.rowHeights = new int[] { 20, 20, 20, 20, 0, 0, 0 };
        gbl_panel.columnWeights = new double[] { 1.0, 1.0, Double.MIN_VALUE };
        gbl_panel.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
        panel.setLayout(gbl_panel);

        JLabel label = new JLabel("Username");
        GridBagConstraints gbc_label = new GridBagConstraints();
        gbc_label.fill = GridBagConstraints.BOTH;
        gbc_label.insets = new Insets(0, 0, 5, 5);
        gbc_label.gridx = 0;
        gbc_label.gridy = 0;
        panel.add(label, gbc_label);

        textFieldUsername = new JTextField();
        textFieldUsername.setEditable(false);
        textFieldUsername.setColumns(10);
        GridBagConstraints gbc_textFieldUsername = new GridBagConstraints();
        gbc_textFieldUsername.fill = GridBagConstraints.BOTH;
        gbc_textFieldUsername.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldUsername.gridx = 1;
        gbc_textFieldUsername.gridy = 0;
        panel.add(textFieldUsername, gbc_textFieldUsername);

        JLabel label_1 = new JLabel("Year of birth");
        GridBagConstraints gbc_label_1 = new GridBagConstraints();
        gbc_label_1.fill = GridBagConstraints.BOTH;
        gbc_label_1.insets = new Insets(0, 0, 5, 5);
        gbc_label_1.gridx = 0;
        gbc_label_1.gridy = 1;
        panel.add(label_1, gbc_label_1);

        textFieldYob = new JTextField();
        textFieldYob.setEditable(false);
        textFieldYob.setColumns(10);
        GridBagConstraints gbc_textFieldYob = new GridBagConstraints();
        gbc_textFieldYob.fill = GridBagConstraints.BOTH;
        gbc_textFieldYob.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldYob.gridx = 1;
        gbc_textFieldYob.gridy = 1;
        panel.add(textFieldYob, gbc_textFieldYob);

        JLabel label_2 = new JLabel("Gender");
        GridBagConstraints gbc_label_2 = new GridBagConstraints();
        gbc_label_2.anchor = GridBagConstraints.WEST;
        gbc_label_2.fill = GridBagConstraints.VERTICAL;
        gbc_label_2.insets = new Insets(0, 0, 5, 5);
        gbc_label_2.gridx = 0;
        gbc_label_2.gridy = 2;
        panel.add(label_2, gbc_label_2);

        textFieldGender = new JTextField();
        textFieldGender.setEnabled(false);
        GridBagConstraints gbc_textFieldGender = new GridBagConstraints();
        gbc_textFieldGender.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldGender.fill = GridBagConstraints.HORIZONTAL;
        gbc_textFieldGender.gridx = 1;
        gbc_textFieldGender.gridy = 2;
        panel.add(textFieldGender, gbc_textFieldGender);
        textFieldGender.setColumns(10);

        JLabel lblAbout = new JLabel("About");
        GridBagConstraints gbc_lblAbout = new GridBagConstraints();
        gbc_lblAbout.fill = GridBagConstraints.BOTH;
        gbc_lblAbout.gridwidth = 2;
        gbc_lblAbout.insets = new Insets(0, 0, 5, 0);
        gbc_lblAbout.gridx = 0;
        gbc_lblAbout.gridy = 3;
        panel.add(lblAbout, gbc_lblAbout);

        textAreaAbout = new JTextArea();
        textAreaAbout.setEditable(false);
        textAreaAbout.setRows(4);
        textAreaAbout.setFont(new Font("Tahoma", Font.PLAIN, 11));
        GridBagConstraints gbc_textAreaAbout = new GridBagConstraints();
        gbc_textAreaAbout.gridheight = 2;
        gbc_textAreaAbout.fill = GridBagConstraints.BOTH;
        gbc_textAreaAbout.gridwidth = 2;
        gbc_textAreaAbout.gridx = 0;
        gbc_textAreaAbout.gridy = 4;
        panel.add(textAreaAbout, gbc_textAreaAbout);

        JLabel lblLooksInterestingGo = new JLabel("Looks interesting? Go to chatroom #nlove in D-Chat and ask for this user!");
        lblLooksInterestingGo.setFont(new Font("Tahoma", Font.BOLD, 11));
        lblLooksInterestingGo.setBounds(10, 203, 414, 14);
        contentPanel.add(lblLooksInterestingGo);
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.LEFT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
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

        fillWithProfile(profile);
    }

    private void fillWithProfile(NloveProfile profile) {
        textFieldUsername.setText(profile.getUsername());
        textFieldYob.setText(String.valueOf(profile.getYearOfBirth()));
        textFieldGender.setText(profile.getGender().toString());
        textAreaAbout.setText(profile.getAbout());

    }

    public JTextArea getTextAreaAbout() {
        return textAreaAbout;
    }
}
