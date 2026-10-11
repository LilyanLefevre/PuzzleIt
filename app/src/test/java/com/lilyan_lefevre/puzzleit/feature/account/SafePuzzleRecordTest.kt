package com.lilyan_lefevre.puzzleit.feature.account

import com.lilyan_lefevre.puzzleit.feature.account.data.isSafePuzzleRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafePuzzleRecordTest {

    @Test
    fun normalIdsPass() {
        assertTrue(isSafePuzzleRecord("3f2a9c1e-7b64-4d0a-9a53-2f1c8e5b7a10", "20261011_101500"))
        assertTrue(isSafePuzzleRecord("e2e-scan-flow", ""))
    }

    @Test
    fun pathTraversalIsRefused() {
        assertFalse(isSafePuzzleRecord("../databases", ""))
        assertFalse(isSafePuzzleRecord("..", ""))
        assertFalse(isSafePuzzleRecord("a/b", ""))
        assertFalse(isSafePuzzleRecord("ok", "../../x"))
        assertFalse(isSafePuzzleRecord("", ""))
    }
}
