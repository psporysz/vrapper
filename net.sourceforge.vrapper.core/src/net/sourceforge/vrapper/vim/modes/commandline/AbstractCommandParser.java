package net.sourceforge.vrapper.vim.modes.commandline;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.ctrlKey;
import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.key;
import static net.sourceforge.vrapper.vim.commands.Utils.characterType;

import java.util.EnumSet;
import java.util.HashMap;

import net.sourceforge.vrapper.keymap.KeyStroke;
import net.sourceforge.vrapper.keymap.KeyStroke.Modifier;
import net.sourceforge.vrapper.keymap.SpecialKey;
import net.sourceforge.vrapper.keymap.vim.SimpleKeyStroke;
import net.sourceforge.vrapper.platform.CommandLineUI;
import net.sourceforge.vrapper.platform.CommandLineUI.CommandLineMode;
import net.sourceforge.vrapper.platform.Platform;
import net.sourceforge.vrapper.utils.ContentType;
import net.sourceforge.vrapper.utils.VimUtils;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.Options;
import net.sourceforge.vrapper.vim.commands.Command;
import net.sourceforge.vrapper.vim.commands.LeaveVisualModeCommand;
import net.sourceforge.vrapper.vim.modes.AbstractVisualMode;
import net.sourceforge.vrapper.vim.modes.ExecuteCommandHint;
import net.sourceforge.vrapper.vim.modes.NormalMode;
import net.sourceforge.vrapper.vim.register.Register;
import net.sourceforge.vrapper.vim.register.RegisterManager;
import net.sourceforge.vrapper.vim.register.StringRegisterContent;

/**
 * Base class for modes which parse strings given by the user.<br>
 * Shows the input in the {@link Platform}'s command line.
 *
 * @author Matthias Radig
 */
public abstract class AbstractCommandParser {

    protected static final KeyStroke KEY_RETURN  = key(SpecialKey.RETURN);
    protected static final KeyStroke KEY_ESCAPE  = key(SpecialKey.ESC);
    protected static final KeyStroke KEY_CTRL_W  = ctrlKey('w');
    protected static final KeyStroke KEY_CTRL_R  = ctrlKey('r');
    protected static final KeyStroke KEY_CTRL_A  = ctrlKey('a');
    protected static final KeyStroke KEY_CTRL_U  = ctrlKey('u');
    protected static final KeyStroke KEY_CTRL_V  = ctrlKey('v');
    protected static final KeyStroke KEY_CTRL_Y  = ctrlKey('y');
    protected static final KeyStroke KEY_BACKSP  = key(SpecialKey.BACKSPACE);
    protected static final KeyStroke KEY_DELETE  = key(SpecialKey.DELETE);
    protected static final KeyStroke KEY_UP      = key(SpecialKey.ARROW_UP);
    protected static final KeyStroke KEY_DOWN    = key(SpecialKey.ARROW_DOWN);
    protected static final KeyStroke KEY_RIGHT   = key(SpecialKey.ARROW_RIGHT);
    protected static final KeyStroke KEY_LEFT    = key(SpecialKey.ARROW_LEFT);
    protected static final KeyStroke KEY_HOME    = key(SpecialKey.HOME);
    protected static final KeyStroke KEY_END     = key(SpecialKey.END);
    protected static final KeyStroke KEY_CTRL_INS= new SimpleKeyStroke(SpecialKey.INSERT, EnumSet.of(Modifier.CONTROL));
    protected static final KeyStroke KEY_SHFT_INS= new SimpleKeyStroke(SpecialKey.INSERT, EnumSet.of(Modifier.SHIFT));
    protected static final KeyStroke KEY_CMD_C   = new SimpleKeyStroke('c', EnumSet.of(Modifier.COMMAND));
    protected static final KeyStroke KEY_CMD_V   = new SimpleKeyStroke('v', EnumSet.of(Modifier.COMMAND));
    protected static final KeyStroke KEY_CMD_X   = new SimpleKeyStroke('x', EnumSet.of(Modifier.COMMAND));

    protected final EditorAdaptor editor;
    private boolean pasteRegister = false;
    protected CommandLineUI commandLine;
    private final CommandLineHistory history = CommandLineHistory.INSTANCE;

    /**
     * Whether the current command is modified and needs to be stored in the command history.
     * Commands restored from history will not be saved again unless edited.
     */
    private boolean modified;
    private boolean isFromVisual = false;
    private boolean isCommandLineHistoryEnabled = true;

    private interface KeyHandler {
        public void handleKey();
    }
    private HashMap<KeyStroke, KeyHandler> editMap = new HashMap<KeyStroke, KeyHandler>();
    {
        editMap.put(KEY_UP, new KeyHandler() { public void handleKey() {
            if (modified)
                history.setTemp(commandLine.getContents());
            String previous = history.getPrevious();
            setCommandFromHistory(previous);
        }});
        editMap.put(KEY_DOWN, new KeyHandler() { public void handleKey() {
            if (modified)
                history.setTemp(commandLine.getContents());
            String next = history.getNext();
            setCommandFromHistory(next);
        }});
        editMap.put(KEY_LEFT, new KeyHandler() { public void handleKey() {
            commandLine.addOffsetToPosition(-1);
        }});
        editMap.put(KEY_RIGHT, new KeyHandler() { public void handleKey() {
            commandLine.addOffsetToPosition(1);
        }});
        editMap.put(KEY_DELETE, new KeyHandler() { public void handleKey() {
            commandLine.delete();
            modified = true;
        }});
        editMap.put(KEY_CTRL_R, new KeyHandler() { public void handleKey() {
            commandLine.setMode(CommandLineMode.REGISTER);
            pasteRegister = true;
            modified = true;
        }});
        editMap.put(KEY_HOME, new KeyHandler() { public void handleKey() {
            commandLine.setPosition(0);
        }});
        editMap.put(KEY_END, new KeyHandler() { public void handleKey() {
            commandLine.setPosition(commandLine.getEndPosition());
        }});
        editMap.put(KEY_CTRL_W, new KeyHandler() { public void handleKey() {
            if(pasteRegister) { //Ctrl+R mode
                String word = VimUtils.getWordUnderCursor(editor, false);
                commandLine.type(word);
            }
            else {
                deleteWordBack();
            }
        }});
        editMap.put(KEY_CTRL_A, new KeyHandler() { public void handleKey() {
            if(pasteRegister) { //Ctrl+R mode
                String word = VimUtils.getWordUnderCursor(editor, true);
                commandLine.type(word);
            }
        }});
        editMap.put(KEY_CTRL_U, new KeyHandler() { public void handleKey() {
            commandLine.replace(0, commandLine.getPosition(), "");
            commandLine.setPosition(0);
            modified = true;
        }});

        editMap.put(KEY_CTRL_Y, new KeyHandler() { public void handleKey() {
            copySelectionToClipboard();
        }});
        editMap.put(KEY_CMD_C, editMap.get(KEY_CTRL_Y));
        editMap.put(KEY_CTRL_INS, editMap.get(KEY_CTRL_Y));

        editMap.put(KEY_CTRL_V, new KeyHandler() { public void handleKey() {
            pasteRegister(RegisterManager.REGISTER_NAME_CLIPBOARD);
        }});
        editMap.put(KEY_SHFT_INS, editMap.get(KEY_CTRL_V));
        editMap.put(KEY_CMD_V, editMap.get(KEY_CTRL_V));

        editMap.put(KEY_CMD_X, new KeyHandler() { public void handleKey() {
            if (commandLine.getSelectionLength() > 0) {
                copySelectionToClipboard();
                commandLine.delete();
            }
        }});
    }


    public AbstractCommandParser(EditorAdaptor vim) {
        this.editor = vim;
        modified = false;
        history.setMode(editor.getCurrentModeName());
    }

    /**
     * Appends typed characters to the internal buffer. Deletes a char from the
     * buffer on press of the backspace key. Parses and executes the buffer on
     * press of the return key. Clears the buffer on press of the escape key.
     * Up/down arrows handle command line history.
     */
    public void type(KeyStroke e) {
        Command c = null;
        KeyHandler mappedHandler = editMap.get(e);
        if (mappedHandler != null) {
            mappedHandler.handleKey();
        } else if (e.equals(KEY_RETURN)) {
            // Disable history if executed through a mapping (nmap X :foobar<CR>).
            if (isHistoryEnabled() && e.isVirtual()) {
                setHistoryEnabled(false);
            }
            c = parseAndExecute();
        } else if (e.getSpecialKey() == SpecialKey.TAB) { //tab-completion for filenames
            String completed = completeArgument(commandLine.getContents(), e);
            if (completed != null) {
                commandLine.resetContents(completed);
            }
            pasteRegister = false;
            return;
        } else if (e.getCharacter() != KeyStroke.SPECIAL_KEY && pasteRegister) {
            String registerName = Character.toString(e.getCharacter());
            pasteRegister(registerName);
            pasteRegister = false;
            modified = true;
        } else if (e.getCharacter() != KeyStroke.SPECIAL_KEY) {
            commandLine.type(Character.toString(e.getCharacter()));
            modified = true;
        }

        //Exit register mode but not command line mode.
        if (pasteRegister && e.equals(KEY_ESCAPE)) {
            pasteRegister = false;
            commandLine.setMode(CommandLineMode.DEFAULT);
        } else if (pasteRegister && ! e.equals(KEY_CTRL_R)) {
            pasteRegister = false;
            commandLine.setMode(CommandLineMode.DEFAULT);
        } else if (e.equals(KEY_BACKSP)) {
            if (commandLine.getContents().length() == 0) {
                handleExit(null);
            } else {
                commandLine.erase();
            }
            modified = true;
        } else if (e.equals(KEY_RETURN) || e.equals(KEY_ESCAPE)) {
            handleExit(c);
        } else {
            if ( ! pasteRegister) {
                commandLine.setMode(CommandLineMode.DEFAULT);
            }
        }
    }

    private void pasteRegister(String registerName) {
        String text = editor.getRegisterManager().getRegister(registerName).getContent().getText();
        text = VimUtils.stripLastNewline(text);
        text = VimUtils.replaceNewLines(text, " ");
        commandLine.type(text);
    }

    private void copySelectionToClipboard() {
        if (commandLine.getSelectionLength() > 0) {
            RegisterManager regMan = editor.getRegisterManager();
            Register clipboard = regMan.getRegister(RegisterManager.REGISTER_NAME_CLIPBOARD);
            String selected = commandLine.getContents(commandLine.getSelectionStart(),
                    commandLine.getSelectionEnd());
            clipboard.setContent(new StringRegisterContent(ContentType.TEXT, selected));
        }
    }

    protected String completeArgument(String commandLineContents, KeyStroke e) {
        return null;
    }

    private void setCommandFromHistory(String cmd) {
        if (cmd == null)
            return;
        modified = false;
        commandLine.resetContents(cmd);
    }

    private void deleteWordBack() {
        int offset = commandLine.getPosition();
    	//Simply backspace if we are at the start or first character
    	if (offset <= 1) {
    	    offset = 0;
    	} else {
    	    String contents = commandLine.getContents();
    	    if (offset > contents.length()) {
    	        offset = contents.length();
    	    }
    	    String iskeyword = editor.getConfiguration().get(Options.KEYWORDS);
    	    char c1, c2;
    	    do {
    	        offset--;
    	        if (offset > 0) {
    	            c1 = contents.charAt(offset - 1);
    	        } else {
    	            break;
    	        }
    	        c2 = contents.charAt(offset);
    	        //this line was stolen from MoveWordLeft because
    	        //I can't call that class with arbitrary text
    	    } while (Character.isWhitespace(c2) || characterType(c1, iskeyword) == characterType(c2, iskeyword));
    	}
    	commandLine.replace(offset, commandLine.getPosition(), "");
    	commandLine.setPosition(offset);
    }

    public boolean isHistoryEnabled() {
        return isCommandLineHistoryEnabled;
    }

    public void setHistoryEnabled(boolean isCommandLineHistoryEnabled) {
        this.isCommandLineHistoryEnabled = isCommandLineHistoryEnabled;
    }

    /**
	 * Parses and executes the given command if possible.
	 * 
	 * @param first
	 *            character used to activate the mode.
	 * @param command
	 *            the command to execute.
	 * @return a command to be executed in normal mode.
	 */
    public abstract Command parseAndExecute(String first, String command);

    /**
     * Runs all exit logic, like switching to normal mode and saving the selection.
     * Implementors may override this function to do custom exit logic.
     * @param parsedCommand this parameter contains the result of the
     *     {@link #parseAndExecute(String, String)} method if <code>&lt;Return&gt;</code> was
     *     pressed. In all other cases it is null.
     */
    protected void handleExit(Command parsedCommand) {
        //Pressing return on an empty command line quits most modes rather than execute a command
        if (parsedCommand == null && isFromVisual) {
            // Fix caret position, clear selection.
            editor.changeModeSafely(NormalMode.NAME,
                    new ExecuteCommandHint.OnEnter(LeaveVisualModeCommand.INSTANCE), AbstractVisualMode.KEEP_SELECTION_HINT);
        } else if (parsedCommand == null) {
            editor.changeModeSafely(NormalMode.NAME);
        } else {
        	isFromVisual = false;
            editor.changeModeSafely(
                    // Return to the last mode in the case of a temporary mode switch.
                    isFromVisual ? NormalMode.NAME : editor.getLastModeName(),
                    new ExecuteCommandHint.OnEnter(parsedCommand), AbstractVisualMode.KEEP_SELECTION_HINT);
            // Only do this AFTER changing the mode, Eclipse commands might still use the selection!
//            editor.setSelection(null);
        }
    }

    private Command parseAndExecute() {
        String first = commandLine.getPrompt();
        String c = commandLine.getContents();
        if (isHistoryEnabled()) {
            history.append(c);
            setHistoryEnabled(false);
        }
        return parseAndExecute(first, c);
    }

    public boolean isFromVisual() {
        return isFromVisual;
    }

    public void setFromVisual(boolean isFromVisual) {
        this.isFromVisual = isFromVisual;
    }

    public void setCommandLine(CommandLineUI commandLine) {
        this.commandLine = commandLine;
        history.setTemp(commandLine.getContents());
    }

}
