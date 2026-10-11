
package edu.compilerlab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineTest {

    @Test
    void foldsConstants() {
        var p = new Pipeline();

        assertEquals(
            "x = 50",
            p.show(p.foldAll(p.parse("x = 10 + 20 * 2;")))
        );
    }

    @Test
    void propagatesConstants() {
        var p = new Pipeline();

        assertEquals(
            "a = 30\nb = 60",
            p.show(p.propagate(
                p.foldAll(p.parse("a = 30; b = a * 2;"))
            ))
        );
    }

    @Test
    void removesUnusedAssignments() {
        var p = new Pipeline();

        assertEquals(
            "a = 1\nresult = a + 3",
            p.show(p.eliminateDead(
                p.parse("a = 1; unused = 2; result = a + 3;")
            ))
        );
    }

    @Test
    void respectsPrecedence() {
        var p = new Pipeline();

        assertEquals(
            "x = 14",
            p.show(p.foldAll(p.parse("x = 2 + 3 * 4;")))
        );
    }

    @Test
    void rejectsBadCharacter() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Pipeline().parse("x = 1 @ 2;")
        );
    }
}
