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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
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
import javax.swing.SwingUtilities;
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
import com.nlove.log.LogUtils;

import jsmith.nknsdk.client.NKNClientException;
import jsmith.nknsdk.client.NKNExplorer;
import jsmith.nknsdk.client.NKNExplorerException;
import jsmith.nknsdk.wallet.WalletException;

public class MainGui {

    private static final Logger LOG = LoggerFactory.getLogger(MainGui.class);
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

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
    private Thread userCountUpdateThread;
    private static JTextArea textAreaStatus;
    private JLabel lblUserCnt;
    private JButton btnOpenHangout;

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
    public MainGui() throws WalletException, NKNClientException {
        initialize();
    }

    private void initializeGui() throws WalletException, NKNClientException {
        String version = MainGui.class.getPackage().getImplementationVersion();
        if (version != null) {
            LOG.info("Nlove version: {}", version);
        }

        if (this.profileManager.profileIsEmpty()) {
            JOptionPane.showMessageDialog(this.frmNloveA, "To use nlove, please create a profile first and press \"Save\"!");
            this.tabbedPaneMain.setSelectedIndex(1);
        }
        setState();
        Runnable periodicTask = new Runnable() {
            public void run() {
                refreshUserCnt();
            }
        };
        executor.scheduleAtFixedRate(periodicTask, 0, 1, TimeUnit.MINUTES);
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

    private void saveProfile() throws WalletException, NKNClientException {

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
            e2.printStackTrace();
            JOptionPane.showMessageDialog(frmNloveA, "Could not save profile, error: " + e2.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        this.setState();
    }

    private void setState() throws WalletException, NKNClientException {
        boolean profileEmpty = this.profileManager.profileIsEmpty();
        this.btnRoll.setEnabled(!profileEmpty);
        this.btnOpenHangout.setEnabled(!profileEmpty);
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
                        btnOpenHangout.setEnabled(false);
                        btnRoll.setEnabled(false);
                        cch.start();
                        refreshUserCnt();
                        frmNloveA.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                        btnRoll.setEnabled(true);
                        btnOpenHangout.setEnabled(true);
                    } catch (WalletException | NKNClientException | NKNExplorerException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }

            });
            clientCommandHandlerThread.start();
        }

    }

    private void refreshUserCnt() {

        try {
            int subCnt = NKNExplorer.Subscription.getSubscriberCount(ClientCommandHandler.LOBBY_TOPIC);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    lblUserCnt.setText(String.valueOf(subCnt));
                    LOG.info(String.format("Updated estimated user count to: %s", subCnt));
                }
            });
        } catch (NKNExplorerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
                } catch (WalletException | NKNClientException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

            @Override
            public void windowClosing(WindowEvent e) {
                frmNloveA.setCursor(new Cursor(Cursor.WAIT_CURSOR));
                executor.shutdown();
                if (cch != null) {
                    cch.stopClient();
                }

                if (rollThread != null) {
                    rollThread.interrupt();
                }
                if (userCountUpdateThread != null) {
                    userCountUpdateThread.interrupt();
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

        JPanel panelRoll = new JPanel();
        tabbedPaneMain.addTab("Roll", null, panelRoll, null);
        panelRoll.setLayout(null);

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
                        } catch (WalletException | InterruptedException | NKNExplorerException e) {
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
        panelRoll.add(btnRoll);

        JLabel lblNewLabel = new JLabel("Welcome to nlove!");
        lblNewLabel.setAlignmentX(1.0f);
        lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblNewLabel.setBounds(2, 0, 108, 23);
        panelRoll.add(lblNewLabel);

        JLabel lblReadyToMeet = new JLabel("Ready to meet new people? Roll now!");
        lblReadyToMeet.setIconTextGap(0);
        lblReadyToMeet.setAlignmentX(1.0f);
        lblReadyToMeet.setVerticalAlignment(SwingConstants.TOP);
        lblReadyToMeet.setBounds(2, 47, 196, 23);
        panelRoll.add(lblReadyToMeet);

        JLabel lblNewLabel_1 = new JLabel("");
        lblNewLabel_1.setIcon(new ImageIcon(MainGui.class.getResource("/resources/logo.png")));
        lblNewLabel_1.setBounds(208, 8, 128, 112);
        panelRoll.add(lblNewLabel_1);

        JLabel lblWeWill = new JLabel("* you will be randomly matched");
        lblWeWill.setFont(new Font("Tahoma", Font.ITALIC, 11));
        lblWeWill.setVerticalAlignment(SwingConstants.TOP);
        lblWeWill.setIconTextGap(0);
        lblWeWill.setAlignmentX(1.0f);
        lblWeWill.setBounds(2, 134, 196, 14);
        panelRoll.add(lblWeWill);

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
        panelRoll.add(lblSponsoredByDappstatcentralml);

        JLabel lblEstimatedSubscriberCount = new JLabel("Estimated online users:");
        lblEstimatedSubscriberCount.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblEstimatedSubscriberCount.setAlignmentX(1.0f);
        lblEstimatedSubscriberCount.setBounds(2, 22, 135, 23);
        panelRoll.add(lblEstimatedSubscriberCount);

        lblUserCnt = new JLabel("UNKNOWN");
        lblUserCnt.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblUserCnt.setAlignmentX(1.0f);
        lblUserCnt.setBounds(140, 22, 128, 23);
        panelRoll.add(lblUserCnt);

        JPanel panelProfile = new JPanel();
        panelProfile.setBorder(new EmptyBorder(2, 2, 2, 2));
        tabbedPaneMain.addTab("Profile", null, panelProfile, null);
        GridBagLayout gbl_panelProfile = new GridBagLayout();
        gbl_panelProfile.columnWidths = new int[] { 99, 246, 0 };
        gbl_panelProfile.rowHeights = new int[] { 20, 20, 20, 20, 0, 0, 0 };
        gbl_panelProfile.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
        gbl_panelProfile.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
        panelProfile.setLayout(gbl_panelProfile);

        JLabel lblUsername = new JLabel("Username");
        GridBagConstraints gbc_lblUsername = new GridBagConstraints();
        gbc_lblUsername.fill = GridBagConstraints.BOTH;
        gbc_lblUsername.insets = new Insets(0, 0, 5, 5);
        gbc_lblUsername.gridx = 0;
        gbc_lblUsername.gridy = 0;
        panelProfile.add(lblUsername, gbc_lblUsername);

        textFieldUsername = new JTextField();
        GridBagConstraints gbc_textFieldUsername = new GridBagConstraints();
        gbc_textFieldUsername.fill = GridBagConstraints.BOTH;
        gbc_textFieldUsername.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldUsername.gridx = 1;
        gbc_textFieldUsername.gridy = 0;
        panelProfile.add(textFieldUsername, gbc_textFieldUsername);
        textFieldUsername.setColumns(10);

        JLabel lblYearOfBirth = new JLabel("Year of birth");
        GridBagConstraints gbc_lblYearOfBirth = new GridBagConstraints();
        gbc_lblYearOfBirth.fill = GridBagConstraints.BOTH;
        gbc_lblYearOfBirth.insets = new Insets(0, 0, 5, 5);
        gbc_lblYearOfBirth.gridx = 0;
        gbc_lblYearOfBirth.gridy = 1;
        panelProfile.add(lblYearOfBirth, gbc_lblYearOfBirth);

        textFieldYearOfBirth = new JTextField();
        textFieldYearOfBirth.setColumns(10);
        GridBagConstraints gbc_textFieldYearOfBirth = new GridBagConstraints();
        gbc_textFieldYearOfBirth.fill = GridBagConstraints.BOTH;
        gbc_textFieldYearOfBirth.insets = new Insets(0, 0, 5, 0);
        gbc_textFieldYearOfBirth.gridx = 1;
        gbc_textFieldYearOfBirth.gridy = 1;
        panelProfile.add(textFieldYearOfBirth, gbc_textFieldYearOfBirth);

        JLabel lblGender = new JLabel("Gender");
        GridBagConstraints gbc_lblGender = new GridBagConstraints();
        gbc_lblGender.fill = GridBagConstraints.BOTH;
        gbc_lblGender.insets = new Insets(0, 0, 5, 5);
        gbc_lblGender.gridx = 0;
        gbc_lblGender.gridy = 2;
        panelProfile.add(lblGender, gbc_lblGender);

        choiceGender = new Choice();
        Arrays.stream(Gender.values()).forEach(g -> choiceGender.add(g.toString()));

        GridBagConstraints gbc_choiceGender = new GridBagConstraints();
        gbc_choiceGender.fill = GridBagConstraints.BOTH;
        gbc_choiceGender.insets = new Insets(0, 0, 5, 0);
        gbc_choiceGender.gridx = 1;
        gbc_choiceGender.gridy = 2;
        panelProfile.add(choiceGender, gbc_choiceGender);

        JLabel lblAboutYou = new JLabel("About you");
        GridBagConstraints gbc_lblAboutYou = new GridBagConstraints();
        gbc_lblAboutYou.gridwidth = 2;
        gbc_lblAboutYou.fill = GridBagConstraints.BOTH;
        gbc_lblAboutYou.insets = new Insets(0, 0, 5, 5);
        gbc_lblAboutYou.gridx = 0;
        gbc_lblAboutYou.gridy = 3;
        panelProfile.add(lblAboutYou, gbc_lblAboutYou);

        textAreaAboutYou = new JTextArea();
        textAreaAboutYou.setRows(4);
        textAreaAboutYou.setFont(new Font("Tahoma", Font.PLAIN, 11));
        GridBagConstraints gbc_textAreaAboutYou = new GridBagConstraints();
        gbc_textAreaAboutYou.insets = new Insets(0, 0, 5, 0);
        gbc_textAreaAboutYou.gridwidth = 2;
        gbc_textAreaAboutYou.fill = GridBagConstraints.BOTH;
        gbc_textAreaAboutYou.gridx = 0;
        gbc_textAreaAboutYou.gridy = 4;
        panelProfile.add(textAreaAboutYou, gbc_textAreaAboutYou);

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
                } catch (WalletException | NKNClientException e1) {
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
        panelProfile.add(btnSave, gbc_btnSave);

        JPanel panelHangout = new JPanel();
        tabbedPaneMain.addTab("Hangout", null, panelHangout, null);
        panelHangout.setLayout(null);

        JLabel lblHangoutZone = new JLabel("Hangout zone");
        lblHangoutZone.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblHangoutZone.setBounds(2, 8, 272, 14);
        panelHangout.add(lblHangoutZone);

        JLabel lblInTheHangout = new JLabel("<html>Free chat zone to meet new acquaintances.</html>");
        lblInTheHangout.setVerticalAlignment(SwingConstants.TOP);
        lblInTheHangout.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblInTheHangout.setBounds(2, 33, 338, 14);
        panelHangout.add(lblInTheHangout);

        btnOpenHangout = new JButton("Open Hangout");
        btnOpenHangout.setEnabled(false);
        btnOpenHangout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            }
        });
        btnOpenHangout.setIcon(new ImageIcon(MainGui.class.getResource("/resources/arrow-right.png")));
        btnOpenHangout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                (new Thread(new Runnable() {
                    public void run() {
                        Thread.currentThread().setName("HangoutDialog");
                        HangoutDialog hd = new HangoutDialog(frmNloveA, true);
                        hd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                        hd.setVisible(true);

                    }
                })).start();

            }
        });
        btnOpenHangout.setBounds(2, 58, 123, 25);
        panelHangout.add(btnOpenHangout);

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

    public JLabel getLblUserCnt() {
        return lblUserCnt;
    }

    public JButton getBtnOpenHangout() {
        return btnOpenHangout;
    }
}
