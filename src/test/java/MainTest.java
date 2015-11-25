import static org.junit.Assert.assertEquals;
import org.junit.Test;

import fr.utc.sr06.CryptokiExplorer.Main;

public class MainTest {
    @Test
    public void add() {
        assertEquals(Main.add(3, 4), 7);
    }
}
