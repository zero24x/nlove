package com.nlove.launcher;

import com.jdotsoft.jarloader.JarClassLoader;

public class NloveLauncher {
    public static void main(String[] args) {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("com.nlove.gui.MainGui", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
