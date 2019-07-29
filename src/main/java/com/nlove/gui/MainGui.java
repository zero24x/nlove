package com.nlove.gui;

import java.awt.Choice;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.darkyen.tproll.TPLogger;
import com.nlove.config.Gender;
import com.nlove.config.NloveProfile;
import com.nlove.config.NloveProfileManager;
import com.nlove.handler.ClientCommandHandler;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.examples.LogUtils;
import jsmith.nknsdk.network.NknHttpApiException;
import jsmith.nknsdk.wallet.WalletException;

public class MainGui {

    private static final Logger LOG = LoggerFactory.getLogger(MainGui.class);

    private JFrame frmNloveA;
    private JTextField textFieldUsername;
    private JTextField textFieldYearOfBirth;

    NloveProfileManager profileManager = NloveProfileManager.INSTANCE;
    private JButton btnRoll;
    private JTextArea textAreaAboutYou;
    private Choice choiceGender;
    private JTabbedPane tabbedPaneMain;

    private ClientCommandHandler cch;

    private Thread clientCommandHandlerThread;
    private Thread rollThread;
    private static JTextArea textAreaStatus;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    MainGui window = new MainGui();
                    window.frmNloveA.setVisible(true);
                    start(args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     * 
     * @throws NknHttpApiException
     * @throws NKNClientException
     * @throws WalletException
     */
    public MainGui() throws WalletException, NKNClientException, NknHttpApiException {
        initialize();
    }

    private void initializeGui() throws WalletException, NKNClientException, NknHttpApiException {
        String version = MainGui.class.getPackage().getImplementationVersion();
        if (version != null) {
            LOG.info("Nlove version: {}", version);
        }

        if (this.profileManager.profileIsEmpty()) {
            JOptionPane.showMessageDialog(this.frmNloveA, "To use nlove, please create a profile first and press \"Save\"!");
            this.tabbedPaneMain.setSelectedIndex(1);
        }
        setState();
    }

    private static void start(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("debug", false, "display debug information");
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = parser.parse(options, args);

        Boolean isDebug = cmd.hasOption("debug") || java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
        LogUtils.setupLogging(isDebug ? TPLogger.DEBUG : TPLogger.INFO, textAreaStatus);

        LOG.info("Loading, please wait...");
    }

    private void saveProfile() throws WalletException, NKNClientException, NknHttpApiException {

        try {
            String username = textFieldUsername.getText();
            if (username.trim().isEmpty()) {
                throw new RuntimeException("Username must not be empty!");
            }

            Gender gender = Gender.valueOf(choiceGender.getSelectedItem());

            Integer yearOfBirth;

            try {
                yearOfBirth = Integer.parseInt(textFieldYearOfBirth.getText());
            } catch (NumberFormatException e2) {
                throw new RuntimeException("Invalid year of birth entered!");
            }

            String aboutText = textAreaAboutYou.getText().trim();
            if (aboutText.length() == 0) {
                throw new RuntimeException("About text must not be empty!");
            }

            NloveProfile profile = new NloveProfile() {
                {
                    setUsername(username);
                    setGender(gender);
                    setYearOfBirth(yearOfBirth);
                    setAbout(aboutText);
                }
            };
            profileManager.setProfile(profile);
            profileManager.saveProfile();
            JOptionPane.showMessageDialog(frmNloveA, "Profile saved, now you can roll!", "Success", JOptionPane.INFORMATION_MESSAGE);
            tabbedPaneMain.setSelectedIndex(0);

        } catch (Exception e2) {
            JOptionPane.showMessageDialog(frmNloveA, "Could not save profile, error: " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        this.setState();
    }

    private void setState() throws WalletException, NKNClientException, NknHttpApiException {
        boolean profileEmpty = this.profileManager.profileIsEmpty();
        this.getBtnRoll().setEnabled(!profileEmpty);
        if (profileEmpty) {
            return;
        }
        loadProfile();

        if (this.cch == null) {
            clientCommandHandlerThread = new Thread(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("ClientCommandHandler");
                    cch = new ClientCommandHandler();
                    try {
                        frmNloveA.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                        btnRoll.setEnabled(false);
                        cch.start();
                        int subCnt = NKNExplorer.getSubscribers(ClientCommandHandler.LOBBY_TOPIC, 0).length;
                        String userCnt = subCnt < 500 ? String.valueOf(subCnt) : String.valueOf(subCnt) + "+";
                        LOG.info(String.format("Registered nlove user count: %s", userCnt));
                        frmNloveA.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        btnRoll.setEnabled(true);
                    } catch (WalletException | NKNClientException | NknHttpApiException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }
            });
            clientCommandHandlerThread.start();

        }

    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmNloveA = new JFrame();
        frmNloveA.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                try {
                    initializeGui();
                } catch (WalletException | NKNClientException | NknHttpApiException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (clientCommandHandlerThread != null) {
                    clientCommandHandlerThread.interrupt();
                }
                if (rollThread != null) {
                    rollThread.interrupt();
                }
            }
        });
        frmNloveA.setIconImage(Toolkit.getDefaultToolkit().getImage(MainGui.class.getResource("/resources/logo.png")));
        frmNloveA.setResizable(false);
        frmNloveA.setTitle("Nlove");
        frmNloveA.setBounds(100, 100, 369, 371);
        frmNloveA.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmNloveA.getContentPane().setLayout(null);

        tabbedPaneMain = new JTabbedPane(JTabbedPane.TOP);
        tabbedPaneMain.setBorder(new EmptyBorder(1, 1, 1, 1));
        tabbedPaneMain.setBounds(2, 0, 351, 196);
        frmNloveA.getContentPane().add(tabbedPaneMain);

        JPanel roll = new JPanel();
        tabbedPaneMain.addTab("Roll", null, roll, null);
        roll.setLayout(null);

        btnRoll = new JButton("Roll");
        btnRoll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        btnRoll.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rollThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            btnRoll.setEnabled(false);
                            cch.roll();
                        } catch (WalletException | InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } finally {
                            btnRoll.setEnabled(true);
                        }
                    }
                });
                rollThread.start();
            }
        });
        btnRoll.setEnabled(false);
        btnRoll.setIcon(new ImageIcon(MainGui.class.getResource("/resources/random.png")));
        btnRoll.setBounds(2, 97, 71, 23);
        roll.add(btnRoll);

        JLabel lblNewLabel = new JLabel("Welcome to nlove!");
        lblNewLabel.setAlignmentX(1.0f);
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblNewLabel.setBounds(2, 0, 108, 23);
        roll.add(lblNewLabel);

        JLabel lblReadyToMeet = new JLabel("Ready to meet new people? Roll now!");
        lblReadyToMeet.setIconTextGap(0);
        lblReadyToMeet.setAlignmentX(1.0f);
        lblReadyToMeet.setVerticalAlignment(SwingConstants.TOP);
        lblReadyToMeet.setBounds(2, 34, 196, 23);
        roll.add(lblReadyToMeet);

        JLabel lblNewLabel_1 = new JLabel("");
        lblNewLabel_1.setIcon(new ImageIcon(MainGui.class.getResource("/resources/logo.png")));
        lblNewLabel_1.setBounds(208, 8, 128, 112);
        roll.add(lblNewLabel_1);

        JLabel lblWeWill = new JLabel("* you will be randomly matched");
        lblWeWill.setVerticalAlignment(SwingConstants.TOP);
        lblWeWill.setIconTextGap(0);
        lblWeWill.setAlignmentX(1.0f);
        lblWeWill.setBounds(2, 68, 196, 23);
        roll.add(lblWeWill);

        JLabel lblSponsoredByDappstatcentralml = new JLabel("<html><a href=''>Sponsored by DappStatCentral.ml</a></html>");
        lblSponsoredByDappstatcentralml.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblSponsoredByDappstatcentralml.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://dappstatcentral.ml"));
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (URISyntaxException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });
        lblSponsoredByDappstatcentralml.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblSponsoredByDappstatcentralml.setForeground(new Color(0, 0, 255));
        lblSponsoredByDappstatcentralml.setBounds(2, 148, 174, 14);
        roll.add(lblSponsoredByDappstatcentralml);

        JPanel profile = new JPanel();
        profile.setBorder(new EmptyBorder(2, 2, 2, 2));
        tabbedPaneMain.addTab("Profile", null, profile, null);
        GridBagLayout gbl_profile = new GridBagLayout();
        gbl_profile.columnWidths = new int[] { 99, 246, 0 };
        gbl_profile.rowHeights = new int[] { 20, 20, 20, 20, 0, 0, 0 };
        gbl_profile.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
        gbl_profile.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
        profile.setLayout(gbl_profile);

        JLabel lblUsername = new JLabel("Username");
        GridBagConstraints gbc_lblUsername = new GridBagConstraints();
        gbc_lblUsername.fill = GridBagConstraints.BOTH;
        gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
        gbc_lblUsername.gridx = 0;
        gbc_lblUsername.gridy = 0;
        profile.add(lblUsername, gbc_lblUsername);

        textFieldUsername = new JTextField();
        GridBagConstraints gbc_textFieldUsername = new GridBagConstraints();
        gbc_textFieldUsername.fill = GridBagConstraints.BOTH;
        gbc_textFieldUsername.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldUsername.gridx = 1;
        gbc_textFieldUsername.gridy = 0;
        profile.add(textFieldUsername, gbc_textFieldUsername);
        textFieldUsername.setColumns(10);

        JLabel lblYearOfBirth = new JLabel("Year of birth");
        GridBagConstraints gbc_lblYearOfBirth = new GridBagConstraints();
        gbc_lblYearOfBirth.fill = GridBagConstraints.BOTH;
        gbc_lblYearOfBirth.insets = new Insets(0, 0, 5, 5);
        gbc_lblYearOfBirth.gridx = 0;
        gbc_lblYearOfBirth.gridy = 1;
        profile.add(lblYearOfBirth, gbc_lblYearOfBirth);

        textFieldYearOfBirth = new JTextField();
        textFieldYearOfBirth.setColumns(10);
        GridBagConstraints gbc_textFieldYearOfBirth = new GridBagConstraints();
        gbc_textFieldYearOfBirth.fill = GridBagConstraints.BOTH;
        gbc_textFieldYearOfBirth.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldYearOfBirth.gridx = 1;
        gbc_textFieldYearOfBirth.gridy = 1;
        profile.add(textFieldYearOfBirth, gbc_textFieldYearOfBirth);

        JLabel lblGender = new JLabel("Gender");
        GridBagConstraints gbc_lblGender = new GridBagConstraints();
        gbc_lblGender.fill = GridBagConstraints.BOTH;
        gbc_lblGender.insets = new Insets(0, 0, 5, 5);
        gbc_lblGender.gridx = 0;
        gbc_lblGender.gridy = 2;
        profile.add(lblGender, gbc_lblGender);

        choiceGender = new Choice();
        Arrays.stream(Gender.values()).forEach(g -> choiceGender.add(g.toString()));

        GridBagConstraints gbc_choiceGender = new GridBagConstraints();
        gbc_choiceGender.fill = GridBagConstraints.BOTH;
        gbc_choiceGender.insets = new Insets(0, 0, 5, 0);
        gbc_choiceGender.gridx = 1;
        gbc_choiceGender.gridy = 2;
        profile.add(choiceGender, gbc_choiceGender);

        JLabel lblAboutYou = new JLabel("About you");
        GridBagConstraints gbc_lblAboutYou = new GridBagConstraints();
        gbc_lblAboutYou.gridwidth = 2;
        gbc_lblAboutYou.fill = GridBagConstraints.BOTH;
        gbc_lblAboutYou.insets = new Insets(0, 0, 5, 5);
        gbc_lblAboutYou.gridx = 0;
        gbc_lblAboutYou.gridy = 3;
        profile.add(lblAboutYou, gbc_lblAboutYou);

        textAreaAboutYou = new JTextArea();
        textAreaAboutYou.setRows(4);
        textAreaAboutYou.setFont(new Font("Tahoma", Font.PLAIN, 11));
        GridBagConstraints gbc_textAreaAboutYou = new GridBagConstraints();
        gbc_textAreaAboutYou.insets = new Insets(0, 0, 5, 0);
        gbc_textAreaAboutYou.gridwidth = 2;
        gbc_textAreaAboutYou.fill = GridBagConstraints.BOTH;
        gbc_textAreaAboutYou.gridx = 0;
        gbc_textAreaAboutYou.gridy = 4;
        profile.add(textAreaAboutYou, gbc_textAreaAboutYou);

        JButton btnSave = new JButton("Save");
        btnSave.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        btnSave.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    saveProfile();
                } catch (WalletException | NKNClientException | NknHttpApiException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        btnSave.setIcon(new ImageIcon(MainGui.class.getResource("/resources/save.png")));
        GridBagConstraints gbc_btnSave = new GridBagConstraints();
        gbc_btnSave.anchor = GridBagConstraints.WEST;
        gbc_btnSave.insets = new Insets(0, 0, 0, 5);
        gbc_btnSave.gridx = 0;
        gbc_btnSave.gridy = 5;
        profile.add(btnSave, gbc_btnSave);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(3, 196, 345, 133);
        frmNloveA.getContentPane().add(scrollPane);

        textAreaStatus = new JTextArea();
        textAreaStatus.setLineWrap(true);
        textAreaStatus.setFont(new Font("Tahoma", Font.PLAIN, 11));
        scrollPane.setViewportView(textAreaStatus);
    }

    private void loadProfile() {
        NloveProfile profile = profileManager.getProfile();
        textFieldUsername.setText(profile.getUsername());
        textFieldYearOfBirth.setText(String.valueOf(profile.getYearOfBirth()));
        choiceGender.select(profile.getGender().toString());
        textAreaAboutYou.setText(profile.getAbout());
    }

    public JButton getBtnRoll() {
        return btnRoll;
    }

    public JTextField getTextFieldYearOfBirth() {
        return textFieldYearOfBirth;
    }

    public JTextArea getTextArea() {
        return textAreaAboutYou;
    }

    public JTextField getTextFieldUsername() {
        return textFieldUsername;
    }

    public Choice getChoiceGender() {
        return choiceGender;
    }

    public JTabbedPane getTabbedPaneMain() {
        return tabbedPaneMain;
    }

    public JTextArea getTextAreaStatus() {
        return textAreaStatus;
    }
}
