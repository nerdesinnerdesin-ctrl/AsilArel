package com.plakoto.engine;

import java.util.List;

/** Dependency-free test runner (no JUnit needed offline). Exit code 1 on any failure. */
public class RulesTest {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL: " + name); } }

    static Board empty() { return new Board(); }
    static void put(Board b, int pl, int pt, int n) { b.owner[pt] = pl; b.count[pt] = n; }
    static boolean has(List<Move> l, int f, int t, int d) { return l.contains(new Move(f, t, d)); }

    public static void main(String[] a) {
        // setup
        Board b = Board.initial();
        check("15 white", b.total(1) == 15); check("15 black", b.total(2) == 15);

        // pinning and blocking
        b = empty(); put(b, 1, 1, 2); put(b, 2, 4, 1); b.owner[24] = 2; b.count[24] = 14;
        check("white can pin lone black", has(b.singleMoves(1, 3), 1, 4, 3));
        b.apply(1, new Move(1, 4, 3));
        check("pin state", b.owner[4] == 1 && b.pinned[4] && b.total(2) == 15);
        Board c = empty(); put(c, 1, 1, 1); put(c, 2, 3, 2);
        check("two checkers block", !has(c.singleMoves(1, 2), 1, 3, 2));

        // pinned checker cannot move; release when pinner leaves
        b = empty(); put(b, 1, 10, 1); b.owner[10] = 1; b.count[10] = 1; b.pinned[10] = true; // black checker pinned under white at 10
        put(b, 2, 12, 1);
        check("pinned black cannot move", b.singleMoves(2, 1).stream().noneMatch(m -> m.from == 10));
        b.apply(1, new Move(10, 11, 1));
        check("released checker becomes owner", b.owner[10] == 2 && b.count[10] == 1 && !b.pinned[10]);
        Board d = empty(); d.owner[5] = 1; d.count[5] = 1; d.pinned[5] = true; put(d, 2, 9, 1);
        check("cannot land on point holding a pin", !has(d.singleMoves(2, 4), 9, 5, 4));

        // bearing off
        b = empty(); put(b, 1, 24, 3); put(b, 1, 20, 1); b.off[1] = 11;
        check("exact bear off", has(b.singleMoves(1, 1), 24, 0, 1));
        check("no overshoot from 24 with checker behind on 20", !has(b.singleMoves(1, 6), 24, 0, 6)); check("farthest checker may overshoot", has(b.singleMoves(1, 6), 20, 0, 6));
        b = empty(); put(b, 1, 24, 2); put(b, 1, 10, 1); b.off[1] = 12;
        check("no bear off when not all home", b.singleMoves(1, 6).stream().noneMatch(m -> m.to == 0));
        b = empty(); put(b, 2, 3, 1); b.off[2] = 14; // black rel(3)=22
        check("black bears off with overshoot", has(b.singleMoves(2, 6), 3, 0, 6));

        // pinned checker counts for 'all home'
        b = empty(); put(b, 1, 24, 1); b.off[1] = 13; b.owner[5] = 2; b.count[5] = 1; b.pinned[5] = true; // white pinned at 5
        check("pinned own checker blocks bear off", b.singleMoves(1, 1).stream().noneMatch(m -> m.to == 0));

        // first turn: two different checkers
        Game g = new Game();
        check("tie repeats", !g.openingRoll(3, 3)); check("higher starts", g.openingRoll(2, 5) && g.current == 2);
        g.roll(3, 1);
        List<Move> lm = g.legalMoves();
        check("first turn offers moves", lm.size() == 2);
        Move m1 = new Move(24, 21, 3);
        check("play first", g.play(m1));
        check("second must be different checker (from start point)", g.legalMoves().stream().allMatch(m -> m.from == 24));
        check("same checker continuing is illegal", !g.legalMoves().contains(new Move(21, 20, 1)));
        check("finish turn", g.play(new Move(24, 23, 1)) && g.current == 1 && g.phase == Game.Phase.ROLL);

        // first turn doubles: still needs 2 checkers
        Game h = new Game(); h.openingRoll(6, 1); h.roll(2, 2);
        check("doubles first turn >=2 checkers", Game.computeSequences(h.board, 2, new int[]{2,2,2,2}, true).stream().noneMatch(s -> Game.singleChecker(h.board, 2, s)));

        // second turn (not first): single checker by sum allowed
        List<List<Move>> seqs = Game.computeSequences(Board.initial(), 1, new int[]{3, 1}, false);
        check("later turn allows same checker", seqs.stream().anyMatch(s -> Game.singleChecker(Board.initial(), 1, s)));

        // first-turn fallback: only one checker can move at all
        b = empty(); put(b, 1, 1, 1); put(b, 2, 24, 1);
        check("fallback plays max legal moves", Game.computeSequences(b, 1, new int[]{3, 1}, true).get(0).size() == 2);

        // max dice and larger die rule
        b = empty(); put(b, 1, 1, 1); put(b, 2, 3, 2); put(b, 2, 24, 13); // die 2 blocked
        List<List<Move>> r = Game.computeSequences(b, 1, new int[]{2, 5}, false);
        check("must play larger die if only one playable", r.stream().allMatch(s -> s.size() == 1 && s.get(0).die == 5) || r.stream().allMatch(s -> s.size() == 2));
        b = empty(); put(b, 1, 2, 1); put(b, 2, 4, 2); put(b, 2, 24, 13); // die 2 blocked at 4; die 6 ok(8); after 6 -> 8, die 2 -> 10 ok
        r = Game.computeSequences(b, 1, new int[]{2, 6}, false);
        check("max dice used", r.stream().allMatch(s -> s.size() == 2));

        // no legal moves => pass, no deadlock
        Game p = new Game(); p.openingRoll(2, 1); // white starts
        p.board = empty(); put(p.board, 1, 1, 1); put(p.board, 2, 2, 2); put(p.board, 2, 3, 2); put(p.board, 2, 24, 11);
        p.roll(1, 2);
        check("blocked player passes, game not stuck", p.phase == Game.Phase.ROLL && p.current == 2);

        // win / mars
        Game w = new Game(); w.openingRoll(1, 6); w.board = empty(); put(w.board, 2, 3, 1); w.board.off[2] = 14; w.board.off[1] = 0; put(w.board, 1, 10, 15);
        w.roll(6, 5);
        boolean played = w.play(new Move(3, 0, 6));
        check("win by bearing off", played && w.phase == Game.Phase.FINISHED && w.winner == 2);
        check("mars scores 2", w.points == 2);
        Game w2 = new Game(); w2.openingRoll(1, 6); w2.board = empty(); put(w2.board, 2, 3, 1); w2.board.off[2] = 14; w2.board.off[1] = 3; put(w2.board, 1, 10, 12);
        w2.roll(6, 5); w2.play(new Move(3, 0, 6));
        check("normal win scores 1", w2.points == 1);

        // invalid move rejected
        Game x = new Game(); x.openingRoll(6, 1); x.roll(3, 1);
        check("illegal move rejected", !x.play(new Move(24, 10, 3)));

        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
