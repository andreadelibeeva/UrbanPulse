package ui.panels;

import analysis.CorrelationEngine;
import analysis.CorrelationEngine.CorrelationResult;
import analysis.PolicyBriefGenerator;
import model.SocialIndicator;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class PolicyBriefPanel extends JPanel{
    private final DataStore dataStore;
    private final CorrelationEngine engine = new CorrelationEngine();
    private final PolicyBriefGenerator generator = new PolicyBriefGenerator();
    private JTextArea briefArea;

    public PolicyBriefGenerator(DataStore dataStore) {

    }
}
