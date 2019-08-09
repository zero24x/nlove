package com.nlove.gui.util;

import java.io.PrintStream;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import com.darkyen.tproll.TPLogger;
import com.darkyen.tproll.logfunctions.SimpleLogFunction;
import com.darkyen.tproll.util.TerminalColor;

public class TextAreaLogFunction extends SimpleLogFunction {

    private PrintStream log_lastStream;

    private volatile JTextArea textArea = null;

    public TextAreaLogFunction(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    protected void logLine(byte level, CharSequence formattedContent) {
        PrintStream out = (level <= TPLogger.INFO || level == TPLogger.LOG || TerminalColor.COLOR_SUPPORTED) ? System.out : System.err;
        if (log_lastStream != out) {
            if (log_lastStream != null) {
                log_lastStream.flush();// To preserve out/err order
            }
            log_lastStream = out;
        }

        String logTxt = new String(formattedContent.toString());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                if (textArea.getLineCount() >= 20) {
                    int end;
                    try {
                        end = textArea.getLineEndOffset(0);
                        textArea.replaceRange("", 0, end);
                    } catch (BadLocationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

                textArea.append(logTxt + "\r\n");
                textArea.setCaretPosition(textArea.getDocument().getLength());

            }
        });
    }
};
