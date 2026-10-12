package com.aashu.infinitycastle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Infinite Mugen-Jou style castle. Everything is computed from coordinates, so every chunk is
 * independent. Runs once per newly generated chunk of the castle dimension (flat void base).
 */
public final class CastleGen {
    private CastleGen() {}

    public static void generate(LevelChunk chunk) {
        new Gen(chunk).run();
    }

    static final int CS = 120;   // horizontal cell size
    static final int CH = 96;    // vertical cell size
    static final int LAYERS = 4;
    static final int HEIGHT = 384;

    static final BlockState SPRUCE = Blocks.SPRUCE_PLANKS.defaultBlockState();
    static final BlockState BIRCH = Blocks.BIRCH_PLANKS.defaultBlockState();
    static final BlockState DOAK = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    static final BlockState RNB = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    static final BlockState GLASS = Blocks.GLASS.defaultBlockState();
    static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    static final BlockState RCONC = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    static final BlockState SEA = Blocks.SEA_LANTERN.defaultBlockState();
    static final BlockState SHROOM = Blocks.SHROOMLIGHT.defaultBlockState();
    static final BlockState CHAIN = Blocks.IRON_CHAIN.defaultBlockState();
    static final BlockState LANT_H = Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true);
    static final BlockState LANT_S = Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, false);
    static final BlockState LANT = Blocks.SOUL_LANTERN.defaultBlockState(); // sentinel, replaced by orientation
    static final BlockState[] ROCK = {
            Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState(),
            Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState()};

    static final class Room {
        int x, y, z, w, d, h, roofL, R, rad;
        boolean spike, curtains;
        int[] perm = {0, 1, 2};
        int[] sg = {1, 1, 1};
    }

    static final int[] HWS = {44, 40, 36, 32, 28, 24, 20, 16, 12, 8};
    static final int TOWER_Y0 = 135, TOWER_H = 10, TOWER_L = 6, TOWER_SP = TOWER_H + 1 + TOWER_L;
    static final List<Room> TOWER = new ArrayList<>();

    static {
        for (int i = 0; i < HWS.length; i++) {
            TOWER.add(tier(HWS[i], TOWER_Y0 + i * TOWER_SP, i == HWS.length - 1, 1, true));
        }
        for (int i = 0; i < 6; i++) {
            TOWER.add(tier(HWS[i], TOWER_Y0 - 1 - i * TOWER_SP, i == 5, -1, false));
        }
    }

    static Room tier(int hw, int y, boolean last, int sgy, boolean curtains) {
        Room r = new Room();
        r.x = 0; r.z = 0; r.y = y; r.w = hw * 2 + 1; r.d = hw * 2 + 1; r.h = TOWER_H;
        r.roofL = last ? 0 : TOWER_L; r.spike = false; r.curtains = curtains;
        r.sg = new int[]{1, sgy, 1};
        r.R = r.w + r.h + 10; r.rad = hw + 6;
        return r;
    }

    // ------------------------------------------------------------------ hashing
    static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    static long hash(long a, long b, long c, long s) {
        return mix(a * 0x632BE59BD9B4E019L ^ mix(b * 0x9E3779B97F4A7C15L ^ mix(c * 0xD1B54A32D192ED03L ^ mix(s + 1234567L))));
    }

    static double rnd(long h) {
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    // ------------------------------------------------------------------ cells
    static Room cellRoom(int cx, int cy, int cz) {
        if ((cx == -1 || cx == 0) && (cz == -1 || cz == 0)) return null; // keep the tower area free
        if (rnd(hash(cx, cy, cz, 1)) > 0.75) return null;
        int[] ws = {15, 17, 19, 21, 25, 29, 33};
        int[] ds = {15, 17, 19, 23, 27};
        int[][] perms = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        Room r = new Room();
        r.w = ws[Math.floorMod(hash(cx, cy, cz, 2), ws.length)];
        r.d = ds[Math.floorMod(hash(cx, cy, cz, 3), ds.length)];
        r.h = 7 + Math.floorMod(hash(cx, cy, cz, 4), 3);
        r.x = cx * CS + 40 + (int) (rnd(hash(cx, cy, cz, 5)) * (CS - 80));
        r.z = cz * CS + 40 + (int) (rnd(hash(cx, cy, cz, 6)) * (CS - 80));
        r.y = cy * CH + 34 + (int) (rnd(hash(cx, cy, cz, 7)) * 32);
        if (rnd(hash(cx, cy, cz, 8)) < 0.4) {
            r.perm = rnd(hash(cx, cy, cz, 9)) < 0.5 ? perms[0] : perms[5];
        } else {
            r.perm = perms[Math.floorMod(hash(cx, cy, cz, 10), 6)];
        }
        long h8 = hash(cx, cy, cz, 11);
        r.sg = new int[]{(h8 & 1) == 0 ? 1 : -1, (h8 & 2) == 0 ? 1 : -1, (h8 & 4) == 0 ? 1 : -1};
        r.spike = true;
        r.R = Math.max(r.w, r.d) + r.h + 10;
        r.rad = Math.max(r.w, r.d) / 2 + 19;
        return r;
    }

    // ------------------------------------------------------------------ one room, one block
    static BlockState roomBlock(Room r, int lx, int ly, int lz) {
        int hw = r.w / 2, hd = r.d / 2, h = r.h;
        int ax = Math.abs(lx), az = Math.abs(lz);
        boolean lanternGrid = lx >= -hw + 3 && lx < hw - 2 && (lx + hw - 3) % 6 == 0
                && lz >= -hd + 3 && lz < hd - 2 && (lz + hd - 3) % 6 == 0;

        if (r.curtains && ly < h) {
            boolean pt = (az == hd + 2 && ax <= hw + 2 && (lx + hw + 2) % 3 == 0)
                    || (ax == hw + 2 && az <= hd + 2 && (lz + hd + 2) % 3 == 0);
            if (pt) {
                int n = 5 + Math.floorMod(mix(lx * 73856093L ^ lz * 19349663L ^ r.y * 83492791L), 12);
                if (ly >= h - n) return CHAIN;
                if (ly == h - 1 - n) return LANT;
            }
        }
        if (ly < 0) {
            if (!r.spike) return null;
            int dep = Math.max(6, Math.min(hw, hd) + 4);
            int k = -ly;
            if (k >= dep) return null;
            int rx = hw - k * hw / dep, rz = hd - k * hd / dep;
            if (ax > rx || az > rz) return null;
            if (ax == rx || az == rz || k == dep - 1) {
                return ROCK[Math.floorMod(mix(lx * 31L + ly * 17L + lz * 13L + r.x * 7919L + r.z * 104729L), 4)];
            }
            return null;
        }
        if (ly == 0) {
            if (ax > hw || az > hd) return null;
            if (lanternGrid) return SEA;
            return (ax == hw || az == hd) ? SPRUCE : BIRCH;
        }
        if (ly == h) {
            if (ax <= hw && az <= hd) return DOAK;
            if (az == hd + 2 && ax <= hw + 2 && (lx + hw + 2) % 3 == 0) return LANT;
            if (ax == hw + 2 && az <= hd + 2 && (lz + hd + 2) % 3 == 0) return LANT;
            return null;
        }
        if (ly < h) {
            if (ly == h - 1 && lanternGrid) return LANT;
            if (az == hd && ax <= hw) {
                if (ly <= 3 && lz == hd && ax <= 1) return null; // door
                return (ax == hw || lx % 4 == 0) ? RNB : GLASS;
            }
            if (ax == hw && az < hd) {
                return (lz % 4 == 0) ? RNB : GLASS;
            }
            return null;
        }
        // roof
        int k = ly - h - 1;
        int rx = hw + 2 - k, rz = hd + 2 - k;
        if (rx < 0 || rz < 0) {
            if (r.roofL > 0) return null;
            int i = k - (Math.min(hw, hd) + 3);
            if (lx == 0 && lz == 0 && i >= 0 && i < 5) return i % 2 == 0 ? GOLD : RNB;
            return null;
        }
        if (r.roofL > 0 && k >= r.roofL) return null;
        boolean solid = (r.roofL > 0 && k == r.roofL - 1) || rx == 0 || rz == 0;
        if (ax > rx || az > rz) return null;
        if (!solid && ax != rx && az != rz) return null;
        return (k == 0 && ax == rx && az == rz) ? GOLD : TILES;
    }

    // ------------------------------------------------------------------ per-chunk generator
    static final class Gen {
        final LevelChunk chunk;
        final int x0, z0;
        final BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        final int[] loc = new int[3];

        Gen(LevelChunk chunk) {
            this.chunk = chunk;
            this.x0 = chunk.getPos().getMinBlockX();
            this.z0 = chunk.getPos().getMinBlockZ();
        }

        void set(int x, int y, int z, BlockState s) {
            if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15 || y < 0 || y >= HEIGHT) return;
            m.set(x, y, z);
            if (chunk.getBlockState(m).isAir()) chunk.setBlockState(m, s, 2);
        }

        void run() {
            for (Room r : TOWER) room(r);

            int ccx = Math.floorDiv(x0 + 8, CS), ccz = Math.floorDiv(z0 + 8, CS);
            for (int cy = 0; cy < LAYERS; cy++)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        Room r = cellRoom(ccx + dx, cy, ccz + dz);
                        if (r != null) room(r);
                    }

            // bridges between neighbour rooms
            for (int cy = 0; cy < LAYERS; cy++)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int cx = ccx + dx, cz = ccz + dz;
                        Room a = cellRoom(cx, cy, cz);
                        if (a == null) continue;
                        Room b = cellRoom(cx + 1, cy, cz);
                        if (b != null) link(a.x, a.y, a.z, a.rad, b.x, b.y, b.z, b.rad);
                        b = cellRoom(cx, cy, cz + 1);
                        if (b != null) link(a.x, a.y, a.z, a.rad, b.x, b.y, b.z, b.rad);
                        if (cy + 1 < LAYERS) {
                            b = cellRoom(cx, cy + 1, cz);
                            if (b != null) link(a.x, a.y, a.z, a.rad, b.x, b.y, b.z, b.rad);
                        }
                    }
            // bridges from the ring around the tower into the tower
            for (int cy = 0; cy < LAYERS; cy++)
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++) {
                        int cx = ccx + dx, cz = ccz + dz;
                        if (cx < -2 || cx > 1 || cz < -2 || cz > 1) continue;
                        if (cx >= -1 && cx <= 0 && cz >= -1 && cz <= 0) continue;
                        Room a = cellRoom(cx, cy, cz);
                        if (a == null || rnd(hash(cx, cy, cz, 30)) > 0.7) continue;
                        int ti = 0, best = Integer.MAX_VALUE;
                        for (int i = 0; i < HWS.length; i++) {
                            int d = Math.abs(TOWER_Y0 + i * TOWER_SP + 5 - a.y);
                            if (d < best) { best = d; ti = i; }
                        }
                        link(a.x, a.y, a.z, a.rad, 0, TOWER_Y0 + ti * TOWER_SP + 4, 0, HWS[ti] + 6);
                    }

            // floating lanterns + torii gates
            for (int cy = 0; cy < LAYERS; cy++)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int cx = ccx + dx, cz = ccz + dz;
                        for (int i = 0; i < 500; i++) {
                            int x = cx * CS + (int) (rnd(hash(cx, cz, i, 40 + cy)) * CS);
                            int z = cz * CS + (int) (rnd(hash(cx, cz, i, 50 + cy)) * CS);
                            if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) continue;
                            int y = cy * CH + (int) (rnd(hash(cx, cz, i, 60 + cy)) * CH);
                            set(x, y, z, LANT_H);
                        }
                        if (!(cx >= -1 && cx <= 0 && cz >= -1 && cz <= 0) && rnd(hash(cx, cy, cz, 20)) < 0.2) {
                            torii(cx * CS + (int) (rnd(hash(cx, cy, cz, 21)) * CS),
                                    cy * CH + 20 + (int) (rnd(hash(cx, cy, cz, 22)) * 40),
                                    cz * CS + (int) (rnd(hash(cx, cy, cz, 23)) * CS),
                                    (hash(cx, cy, cz, 24) & 1) == 0);
                        }
                    }
        }

        void room(Room r) {
            int minx = Math.max(x0, r.x - r.R), maxx = Math.min(x0 + 15, r.x + r.R);
            int minz = Math.max(z0, r.z - r.R), maxz = Math.min(z0 + 15, r.z + r.R);
            int miny = Math.max(0, r.y - r.R), maxy = Math.min(HEIGHT - 1, r.y + r.R);
            if (minx > maxx || minz > maxz || miny > maxy) return;
            boolean upright = r.perm[1] == 1;
            BlockState lantern = upright ? (r.sg[1] == 1 ? LANT_H : LANT_S) : SHROOM;
            for (int x = minx; x <= maxx; x++)
                for (int z = minz; z <= maxz; z++)
                    for (int y = miny; y <= maxy; y++) {
                        loc[r.perm[0]] = r.sg[0] * (x - r.x);
                        loc[r.perm[1]] = r.sg[1] * (y - r.y);
                        loc[r.perm[2]] = r.sg[2] * (z - r.z);
                        BlockState s = roomBlock(r, loc[0], loc[1], loc[2]);
                        if (s == null) continue;
                        if (s == LANT) s = lantern;
                        else if (s == CHAIN && !upright) continue;
                        set(x, y, z, s);
                    }
        }

        void link(int ax, int ay, int az, int ar, int bx, int by, int bz, int br) {
            if (Math.max(ax, bx) + 4 < x0 || Math.min(ax, bx) - 4 > x0 + 15) return;
            if (Math.max(az, bz) + 4 < z0 || Math.min(az, bz) - 4 > z0 + 15) return;
            int dx = bx - ax, dy = by - ay, dz = bz - az;
            int n = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
            for (int i = 0; i <= n; i++) {
                double t = i / (double) Math.max(n, 1);
                double px = ax + dx * t, py = ay + dy * t, pz = az + dz * t;
                if (dist(px - ax, py - ay, pz - az) < ar || dist(px - bx, py - by, pz - bz) < br) continue;
                int x = (int) Math.round(px), y = (int) Math.round(py), z = (int) Math.round(pz);
                if (x < x0 - 3 || x > x0 + 18 || z < z0 - 3 || z > z0 + 18) continue;
                BlockState deck = i % 11 == 0 ? SEA : (i % 2 == 1 ? SPRUCE : DOAK);
                set(x, y, z, deck);
                set(x + 1, y, z, deck); set(x - 1, y, z, deck); set(x, y, z + 1, deck); set(x, y, z - 1, deck);
                set(x + 2, y, z, deck); set(x - 2, y, z, deck); set(x, y, z + 2, deck); set(x, y, z - 2, deck);
                if (i % 6 == 0) {
                    set(x + 3, y + 1, z, RNB); set(x + 3, y + 2, z, LANT_S);
                    set(x - 3, y + 1, z, RNB); set(x - 3, y + 2, z, LANT_S);
                }
                if (i % 14 == 7) {
                    set(x, y - 1, z, CHAIN); set(x, y - 2, z, LANT_H);
                }
            }
        }

        static double dist(double a, double b, double c) {
            return Math.sqrt(a * a + b * b + c * c);
        }

        void torii(int x, int y, int z, boolean axisX) {
            if (axisX) { if (x + 4 < x0 || x - 2 > x0 + 15 || z + 14 < z0 || z - 14 > z0 + 15) return; }
            else { if (z + 4 < z0 || z - 2 > z0 + 15 || x + 14 < x0 || x - 14 > x0 + 15) return; }
            for (int dz = -13; dz <= 13; dz++) {
                for (int t = -1; t <= 3; t++) tset(x, y, z, axisX, t, 19, dz, TILES);
                for (int t = 0; t <= 2; t++) tset(x, y, z, axisX, t, 20, dz, Math.abs(dz) > 6 ? TILES : GOLD);
                if (Math.abs(dz) <= 9) for (int t = 0; t <= 1; t++) tset(x, y, z, axisX, t, 15, dz, RCONC);
            }
            for (int dz : new int[]{-9, 9})
                for (int yy = 0; yy < 19; yy++)
                    for (int t = 0; t <= 2; t++) tset(x, y, z, axisX, t, yy, dz, RCONC);
        }

        void tset(int x, int y, int z, boolean axisX, int t, int yy, int dz, BlockState s) {
            if (axisX) set(x + t, y + yy, z + dz, s); else set(x + dz, y + yy, z + t, s);
        }
    }
}
