package com.plakoto.engine;

import java.util.ArrayList;
import java.util.List;

/** Plakoto match state machine. Dice are injected (server supplies them) so the engine is deterministic and testable. */
public final class Game {
    public enum Phase { OPENING, ROLL, MOVE, FINISHED }

    public Board board = Board.initial();
    public Phase phase = Phase.OPENING;
    public int current = 0, winner = 0, points = 0;
    public int[] dice = new int[0];
    private final boolean[] firstTurnDone = new boolean[3];
    private List<List<Move>> candidates = new ArrayList<>();
    private final List<Move> played = new ArrayList<>();

    /** Opening dice are separate from move dice and never played. Returns true if a starter was decided. */
    public boolean openingRoll(int white, int black) {
        if (phase != Phase.OPENING) throw new IllegalStateException("not opening");
        if (white == black) return false;
        current = white > black ? Board.WHITE : Board.BLACK;
        phase = Phase.ROLL;
        return true;
    }

    public void roll(int d1, int d2) {
        if (phase != Phase.ROLL) throw new IllegalStateException("not roll phase");
        dice = d1 == d2 ? new int[]{d1, d1, d1, d1} : new int[]{d1, d2};
        played.clear();
        candidates = computeSequences(board, current, dice, !firstTurnDone[current]);
        phase = Phase.MOVE;
        if (candidates.size() == 1 && candidates.get(0).isEmpty()) endTurn(); // no legal move: pass
    }

    /** Next moves allowed right now (prefix-consistent with some legal full sequence). */
    public List<Move> legalMoves() {
        List<Move> out = new ArrayList<>();
        if (phase != Phase.MOVE) return out;
        for (List<Move> s : candidates)
            if (s.size() > played.size() && s.subList(0, played.size()).equals(played)) {
                Move n = s.get(played.size());
                if (!out.contains(n)) out.add(n);
            }
        return out;
    }

    public boolean play(Move m) {
        if (!legalMoves().contains(m)) return false;
        board.apply(current, m);
        played.add(m);
        if (board.off[current] == 15) { winner = current; points = board.off[Board.other(current)] == 0 ? 2 : 1; phase = Phase.FINISHED; return true; }
        if (legalMoves().isEmpty()) endTurn();
        return true;
    }

    private void endTurn() {
        firstTurnDone[current] = true;
        current = Board.other(current);
        phase = Phase.ROLL; dice = new int[0]; played.clear(); candidates = new ArrayList<>();
    }

    // ---- turn-level rule resolution ----
    public static List<List<Move>> computeSequences(Board b, int p, int[] dice, boolean firstTurn) {
        List<Integer> rem = new ArrayList<>();
        for (int d : dice) rem.add(d);
        List<List<Move>> leaves = new ArrayList<>();
        gen(b, p, rem, new ArrayList<>(), leaves);
        int max = 0;
        for (List<Move> s : leaves) max = Math.max(max, s.size());
        List<List<Move>> keep = new ArrayList<>();
        if (firstTurn) { // two different checkers required whenever the legal moves allow it
            int mmax = 0;
            for (List<Move> s : leaves) if (!singleChecker(b, p, s)) mmax = Math.max(mmax, s.size());
            if (mmax >= 2) { for (List<Move> s : leaves) if (!singleChecker(b, p, s) && s.size() == mmax) keep.add(s); }
        }
        if (keep.isEmpty()) for (List<Move> s : leaves) if (s.size() == max) keep.add(s);
        if (max == 1 && dice.length == 2 && dice[0] != dice[1]) { // only one die playable: must be the larger if possible
            int big = Math.max(dice[0], dice[1]);
            List<List<Move>> larger = new ArrayList<>();
            for (List<Move> s : keep) if (s.get(0).die == big) larger.add(s);
            if (!larger.isEmpty()) keep = larger;
        }
        return keep;
    }

    private static void gen(Board b, int p, List<Integer> rem, List<Move> cur, List<List<Move>> out) {
        boolean extended = false;
        List<Integer> tried = new ArrayList<>();
        for (int i = 0; i < rem.size(); i++) {
            int d = rem.get(i);
            if (tried.contains(d)) continue;
            tried.add(d);
            for (Move m : b.singleMoves(p, d)) {
                extended = true;
                Board nb = b.copy(); nb.apply(p, m);
                List<Integer> r2 = new ArrayList<>(rem); r2.remove(i);
                cur.add(m); gen(nb, p, r2, cur, out); cur.remove(cur.size() - 1);
            }
        }
        if (!extended) out.add(new ArrayList<>(cur));
    }

    /** True if the whole sequence can only be one checker walking (e.g. one checker moved by the sum of both dice). */
    static boolean singleChecker(Board start, int p, List<Move> seq) {
        if (seq.size() < 2) return true;
        Board b = start.copy();
        int prevCountBefore = 0;
        for (int i = 0; i < seq.size(); i++) {
            Move m = seq.get(i);
            if (i > 0) {
                Move prev = seq.get(i - 1);
                if (m.from != prev.to || prevCountBefore > 0) return false;
            }
            prevCountBefore = m.to == 0 ? 0 : b.checkersAt(p, m.to);
            b.apply(p, m);
        }
        return true;
    }
}
