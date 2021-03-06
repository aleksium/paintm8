import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ShoutBox extends JPanel implements ActionListener {
    protected JTextField textField_;
    protected JTextArea textArea_;
    private final static String newline = "\n";
    private ClientSend client_;

    public ShoutBox(ClientSend client) {
        super(new GridBagLayout());
        textField_ = new JTextField(0);
        textField_.addActionListener(this);
        textArea_ = new JTextArea(5, 0);
        textArea_.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea_);
        client_ = client;

        //Add Components to this panel.
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        c.fill = GridBagConstraints.HORIZONTAL;
        add(textField_, c);

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        add(scrollPane, c);
    }

    public void addStatusText(String text) {
        textArea_.append(text + newline);
        textArea_.setCaretPosition(textArea_.getDocument().getLength());
    }

    public void actionPerformed(ActionEvent evt) {
        String text = textField_.getText();
        textField_.setText("");

        //Make sure the new text is visible, even if there
        //was a selection in the text area.
        textArea_.setCaretPosition(textArea_.getDocument().getLength());
        client_.fileReport(text);
    }
}