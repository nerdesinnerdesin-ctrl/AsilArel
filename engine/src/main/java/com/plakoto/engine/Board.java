package com.plakoto.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Plakoto board. Players: 1 = WHITE (moves 1->24, home 19..24), 2 = BLACK (moves 24->1, home 1..6).
 * Each point: top owner, count of owner's checkers, and pinned flag = one opponent checker lies beneath.
 */
public final class Board {
    public static final int WHITE = 1, BLACK = 2;
    public final int[] owner = new int[25];
    public final int[] count = new int[25];
    public final boolean[] pinned = new boolean[25];
    public final int[] off = new int[3];

    public static int other(int p) { return p == WHITE ? BLACK : WHITE; }
    /** Position of point from player's own perspective (1 = start, 24 = last point of home). */
    public static int rel(int p, int point) { return p == WHITE ? point : 25 - point; }

    public static Board initial() {
        Board b = new Board();
        b.owner[1] = WHITE; b.count[1] = 15;
        b.owner[24] = BLACK; b.count[24] = 15;
        return b;
    }

    public Board copy() {
        Board b = new Board();
        System.arraycopy(owner, 0, b.owner, 0, 25);
        System.arraycopy(count, 0, b.count, 0, 25);
        System.arraycopy(pinned, 0, b.pinned, 0, 25);
        System.arraycopy(off, 0, b.off, 0, 3);
        return b;
    }

    /** Checkers of player p on a point, including a pinned one beneath. */
    public int checkersAt(int p, int point) {
        int n = owner[point] == p ? count[point] : 0;
        if (pinned[point] && owner[point] == other(p)) n++;
        return n;
    }

    public int total(int p) {
        int n = off[p];
        for (int i = 1; i <= 24; i++) n += checkersAt(p, i);
        return n;
    }

    public boolean allInHome(int p) {
        for (int i = 1; i <= 24; i++) if (checkersAt(p, i) > 0 && rel(p, i) < 19) return false;
        return true;
    }

    private boolean canLand(int p, int q) {
        if (owner[q] == 0 || owner[q] == p) return true;
        return count[q] == 1 && !pinned[q];
    }

    /** All legal single moves with die d (ignores turn-level rules). */
    public List<Move> singleMoves(int p, int d) {
        List<Move> out = new ArrayList<>();
        for (int pt = 1; pt <= 24; pt++) {
            if (owner[pt] != p || count[pt] == 0) continue; // pinned checker is never the owner, so it can't move
            int r = rel(p, pt), t = r + d;
            if (t <= 24) {
                int q = p == WHITE ? t : 25 - t;
                if (canLand(p, q)) out.add(new Move(pt, q, d));
            } else if (allInHome(p)) {
                boolean behind = false;
                for (int i = 1; i <= 24; i++) if (checkersAt(p, i) > 0 && rel(p, i) < r) { behind = true; break; }
                if (t == 25 || !behind) out.add(new Move(pt, 0, d));
            }
        }
        return out;
    }

    public void apply(int p, Move m) {
        if (--count[m.from] == 0) {
            if (pinned[m.from]) { owner[m.from] = other(p); count[m.from] = 1; pinned[m.from] = false; }
            else owner[m.from] = 0;
        }
        if (m.to == 0) { off[p]++; return; }
        if (owner[m.to] == 0) { owner[m.to] = p; count[m.to] = 1; }
        else if (owner[m.to] == p) count[m.to]++;
        else { pinned[m.to] = true; owner[m.to] = p; count[m.to] = 1; } // pin the lone opponent checker
    }
}
