package com.plakoto.engine;

/** One checker move. from/to are points 1..24; to==0 means bear off. */
public final class Move {
    public final int from, to, die;
    public Move(int from, int to, int die) { this.from = from; this.to = to; this.die = die; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof Move)) return false;
        Move m = (Move) o; return from == m.from && to == m.to && die == m.die;
    }
    @Override public int hashCode() { return from * 961 + to * 31 + die; }
    @Override public String toString() { return from + "->" + (to == 0 ? "off" : String.valueOf(to)) + "(" + die + ")"; }
}
