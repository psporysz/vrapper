package net.sourceforge.vrapper.core.tests.cases;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.parseKeyStrokes;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import net.sourceforge.vrapper.core.tests.utils.CommandTestCase;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.VimUtils;
import net.sourceforge.vrapper.vim.modes.NormalMode;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for remapping in different modes.
 */
public class RemappingTests extends CommandTestCase {

    @Override
    public void setUp() {
        super.setUp();
        adaptor.changeModeSafely(NormalMode.NAME);
    }

    @Override
    protected void reloadEditorAdaptor() {
        super.reloadEditorAdaptor();
        adaptor.changeModeSafely(NormalMode.NAME);
    }

    /** Clears the mapping before and after all tests to make sure other tests aren't impacted. */
    @Before
    @After
    public void clearMappings() {
        super.cleanUp();
        type(parseKeyStrokes(":nmapclear<CR>"));
        type(parseKeyStrokes(":omapclear<CR>"));
        type(parseKeyStrokes(":vmapclear<CR>"));
        type(parseKeyStrokes(":imapclear<CR>"));
    }

    @Test
    public void testCountingRemap() {
        type(parseKeyStrokes(":nmap L dd<CR>"));
        checkCommand(forKeySeq("3L"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'j', "kl");
    }

    @Test
    public void testOmap() {
        // Sanity checks
        checkCommand(forKeySeq("d$"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\ndef\nghi\njkl\nm");
        checkCommand(forKeySeq("g~$"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "aB", 'C', "\ndef\nghi\njkl\nm");

        type(parseKeyStrokes(":onoremap L $<CR>"));
        checkCommand(forKeySeq("dL"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\ndef\nghi\njkl\nm");
        checkCommand(forKeySeq("g~L"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "aB", 'C', "\ndef\nghi\njkl\nm");
    }

    @Test
    public void testCountingOmap() {
        // Sanity check
        checkCommand(forKeySeq("2\"_2d$"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\nm");
        checkCommand(forKeySeq("2g~2$"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "aBC\nDEF\nGHI\nJK", 'L', "\nm");

        type(parseKeyStrokes(":onoremap L $<CR>"));
        checkCommand(forKeySeq("2\"_2dL"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\nm");
        checkCommand(forKeySeq("d4L"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\nm");
        checkCommand(forKeySeq("\"_d4L"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'a', "\nm");
        checkCommand(forKeySeq("2g~2L"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "aBC\nDEF\nGHI\nJK", 'L', "\nm");
    }

    @Test
    public void testBlackHoleRegisterRemap() {

        // Sanity check
        checkCommand(forKeySeq("\"_dd"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'd', "ef\nghi\njkl");
        type(parseKeyStrokes(":nnoremap R \"_dd<CR>"));
        checkCommand(forKeySeq("R"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'd', "ef\nghi\njkl");

        type(parseKeyStrokes(":nnoremap D \"_d$<CR>"));
        checkCommand(forKeySeq("D"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'a', "\ndef\nghi\njkl");
    }

    @Test
    public void testBlackHoleRegisterCountingRemap() {
        // Sanity checks
        checkCommand(forKeySeq("\"_3dd"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'j', "kl");
        checkCommand(forKeySeq("3\"_dd"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'j', "kl");

        type(parseKeyStrokes(":noremap dd \"_dd<CR>"));
        checkCommand(forKeySeq("3dd"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'j', "kl");
        type(parseKeyStrokes(":noremap dd \"_2dd<CR>"));
        checkCommand(forKeySeq("2dd"),
                "a", 'b', "c\ndef\nghi\njkl\nm",
                "", 'm', "");

        type(parseKeyStrokes(":nnoremap D \"_d$<CR>"));
        checkCommand(forKeySeq("D"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'a', "\ndef\nghi\njkl");
        checkCommand(forKeySeq("2D"),
                "a", 'b', "c\ndef\nghi\njkl",
                "", 'a', "\nghi\njkl");
    }

    @Test
    public void testPrefixTextObject() {
        checkCommand(forKeySeq("di)"),
                "(", 'a', "bc\ndef\ngh)\njkl",
                "(", ')', "\njkl");
        type(parseKeyStrokes(":nnoremap )) j<CR>"));
        checkCommand(forKeySeq("di)"),
                "(", 'a', "bc\ndef\ngh)\njkl",
                "(", ')', "\njkl");
        type(parseKeyStrokes(":omap )) j<CR>"));
        checkCommand(forKeySeq("d))"),
                "(", 'a', "bc\ndef\ngh)\njkl",
                "", 'g', "h)\njkl");
        checkCommand(forKeySeq("di)"),
                "(", 'a', "bc\ndef\ngh)\njkl",
                "(", ')', "\njkl");
    }

    @Test
    public void testEndOfLineRemap() {
        // Original bug report used 'L', but that is too difficult to test as L is 'jump to middle'
        type(parseKeyStrokes(":nnoremap l $<CR>"));
        checkCommand(forKeySeq("l"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "[ab", 'c', "\ndef\ngh]\njkl");

        // Should use omap, but 'l' isn't defined yet so it should delete just one char at first.
        checkCommand(forKeySeq("dl"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "[", 'b', "c\ndef\ngh]\njkl");
        type(parseKeyStrokes(":onoremap l $<CR>"));
        // Try again now that l is in omap.
        checkCommand(forKeySeq("dl"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "", '[', "\ndef\ngh]\njkl");
        // Deletes till end of current line and next line.
        checkCommand(forKeySeq("d2l"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "", '[', "\ngh]\njkl");
        checkCommand(forKeySeq("2dl"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "", '[', "\ngh]\njkl");
        // Should delete till end of current line + 7 more lines
        checkCommand(forKeySeq("2\"_2d2l"),
                "[", 'a', "bc\ndef\ngh\njkl\nmno\npqr\nstu\nvwx\nyza\nbcd\nefg",
                "", '[', "\nyza\nbcd\nefg");

        // 'l' isn't remapped yet in visual mode. Try before / after.
        checkCommand(forKeySeq("vld"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "[", 'c', "\ndef\ngh]\njkl");
        type(parseKeyStrokes(":vnoremap l $<CR>"));
        checkCommand(forKeySeq("vld"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "[", 'd', "ef\ngh]\njkl");
    }

    @Test
    public void testMotionsWithoutOMap() {
        // Check that no omap binds are mixed in with the prefixed motions: f,F,t,T,`,',i,a
        checkCommand(forKeySeq("fL"),
                "old", ' ', "McDonnaLD had some $",
                "old McDonna", 'L', "D had some $");
        type(parseKeyStrokes(":noremap L $<CR>"));
        checkCommand(forKeySeq("L"),
                "[", 'a', "bc\ndef\ngh]\njkl",
                "[ab", 'c', "\ndef\ngh]\njkl");
        checkCommand(forKeySeq("fL"),
                "old", ' ', "McDonnaLD had some $",
                "old McDonna", 'L', "D had some $");
        checkCommand(forKeySeq("dfL"),
                "old", ' ', "McDonnaLD had some $LLaz",
                "old", 'D', " had some $LLaz");
        checkCommand(forKeySeq("d2fL"),
                "old", ' ', "McDonnaLD had some $LLaz",
                "old", 'L', "az");

        // Test cursor service doesn't support marks, just check if they're set and get.
        type(parseKeyStrokes(":noremap s \"_s<CR>"));
        type(parseKeyStrokes("ms"));
        verify(cursorAndSelection, times(1)).setMark(Mockito.eq("s"), Mockito.<Position>any());
        type(parseKeyStrokes("'s"));
        verify(cursorAndSelection, times(1)).getMark("s");

        checkCommand(forKeySeq("dams"),
                "so old", ' ', "McDonnaLD had some $LLaz",
                "", 'o', "me $LLaz");

        // sanity check
        checkCommand(forKeySeq("df0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "so old", 'n', "naLD had some $LLaz");

        type(parseKeyStrokes(":onoremap 0 0x<CR>"));
        checkCommand(forKeySeq("d0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "", 'M', "cD0nnaLD had some $LLaz");
        checkCommand(forKeySeq("df0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "so old", 'n', "naLD had some $LLaz");
    }
    
    @Test
    public void testPrefixCommandMap() {
        // Sanity check
        checkCommand(forKeySeq("gq2j"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "", ' ', "   heey oldMcDonnaLD had some $ ia\nia\no");

        // 'q' mapping should only be invoked in operator mode or at start of command.
        type(parseKeyStrokes(":noremap q 2j<CR>"));
        checkCommand(forKeySeq("gq2j"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "", ' ', "   heey oldMcDonnaLD had some $ ia\nia\no");
        checkCommand(forKeySeq("gqq"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "", ' ', "   heey oldMcDonnaLD had some $ ia\nia\no");
        checkCommand(forKeySeq("q"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    heey\noldMcDonnaLD had some $\ni", 'a', "\nia\no");
    }
    
    @Test
    public void testRemapShouldNotShadeOriginalCommand() {
        // Sanity check
        checkCommand(forKeySeq("gg"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    ", 'h', "eey oldMcDonnaLD had some $\nia\nia\no");
        checkCommand(forKeySeq("G"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    heey oldMcDonnaLD had some $\nia\nia\n", 'o', "");

        // Both commands should work
        type(parseKeyStrokes(":noremap gr G<CR>"));
        checkCommand(forKeySeq("gg<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    ", 'h', "eey oldMcDonnaLD had some $\nia\nia\no");
        checkCommand(forKeySeq("gr<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    heey oldMcDonnaLD had some $\nia\nia\n", 'o', "");
        checkCommand(forKeySeq("gg<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    ", 'h', "eey oldMcDonnaLD had some $\nia\nia\no");
        checkCommand(forKeySeq("grgg<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    ", 'h', "eey oldMcDonnaLD had some $\nia\nia\no");
        checkCommand(forKeySeq("grgggr<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    heey oldMcDonnaLD had some $\nia\nia\n", 'o', "");
        checkCommand(forKeySeq("grxggxgr<ESC>"),
                "    h", 'e', "ey oldMcDonnaLD had some $\nia\nia\no",
                "    eey oldMcDonnaLD had some $\nia\nia\n", EOF, "");
    }

    @Test
    public void testRemapZero() {
        // Sanity checks
        checkCommand(forKeySeq("d0"),
                "    so old", ' ', "McD0nnaLD had some $LLaz",
                "", ' ', "McD0nnaLD had some $LLaz");
        checkCommand(forKeySeq("d^"),
                "    so old", ' ', "McD0nnaLD had some $LLaz",
                "    ", ' ', "McD0nnaLD had some $LLaz");

        type(parseKeyStrokes(":map 0 ^<CR>"));
        checkCommand(forKeySeq("d0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "", ' ', "McD0nnaLD had some $LLaz");
        checkCommand(forKeySeq("d0"),
                "    so old", ' ', "McD0nnaLD had some $LLaz",
                "    ", ' ', "McD0nnaLD had some $LLaz");
        checkCommand(forKeySeq("df0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "so old", 'n', "naLD had some $LLaz");
        checkCommand(forKeySeq("d10l"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "so old", ' ', "had some $LLaz");

        /*
         * Copied from NormalModeTests.testPercent
         */
        // Shouldn't do anything
        checkCommand(forKeySeq("500%"),
                "1\n",'2',"\n3\n4\n5\n6\n7\n8\n9\n10\n",
                "1\n",'2',"\n3\n4\n5\n6\n7\n8\n9\n10\n");
        checkCommand(forKeySeq("100%"),
                "1\n",'2',"\n3\n4\n5\n6\n7\n8\n9\n10",
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n",'1',"0");
        // Should go to first non-whitespace character
        checkCommand(forKeySeq("100%"),
                "1\n",'2',"\n3\n4\n5\n6\n7\n8\n9\n    10",
                "1\n2\n3\n4\n5\n6\n7\n8\n9\n    ",'1',"0");
        checkCommand(forKeySeq("%"),
                "fun",'(',"call);",
                "fun(call",')',";");
        checkCommand(forKeySeq("%"),
                "fun(call",')',";",
                "fun",'(',"call);");

        checkCommand(forKeySeq("df0"),
                "so old", ' ', "McD0nnaLD had some $LLaz",
                "so old", 'n', "naLD had some $LLaz");
    }

    @Test
    public void testInsertModeRemap() {
        // Quick sanity check
        checkCommand(forKeySeq("aya<esc>"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    hey", 'a', "ey\noldMcDonnaLD had some $\nia\nia\no");

        // Test that 'jj' quits (only after remapping)
        checkCommand(forKeySeq("ijkjjlh<ESC>"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    hjkjjl", 'h', "eey\noldMcDonnaLD had some $\nia\nia\no");
        type(parseKeyStrokes(":inoremap jj <LT>ESC<GT><CR>")); // Double escaping for test key parse
        checkCommand(forKeySeq("ijkjjlh<ESC>"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    hj", 'k', "eey\noldMcDonnaLD had some $\nia\nia\no");

        // Test that initially failed 'jj' mapping allows kk mapping to be detected by backtracking
        checkCommand(forKeySeq("ijkklhjjlh"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    hjkkl", 'h', "eey\noldMcDonnaLD had some $\nia\nia\no");
        type(parseKeyStrokes(":inoremap kk <LT>ESC<GT><CR>"));
        checkCommand(forKeySeq("ijkklh<ESC>"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    h", 'j', "eey\noldMcDonnaLD had some $\nia\nia\no");
    }

    @Test
    public void testRecursiveRemap() {
        type(parseKeyStrokes(":nnoremap Z ex<CR>"));
        type(parseKeyStrokes(":nmap gz ggZbx<CR>"));
        checkCommand(forKeySeq("gz"),
                "    h", 'e', "ey\noldMcDonnaLD had some $\nia\nia\no",
                "    ", 'e', "e\noldMcDonnaLD had some $\nia\nia\no");
    }
}
