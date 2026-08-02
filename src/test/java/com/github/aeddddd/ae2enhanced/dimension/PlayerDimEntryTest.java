package com.github.aeddddd.ae2enhanced.dimension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;

/**
 * {@link PlayerDimEntry} 的 NBT 序列化容错与权限管理契约测试。
 */
public class PlayerDimEntryTest {

    private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GUEST_A = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID GUEST_B = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /** 新条目默认值：维度 ID 为 MIN_VALUE（未分配）、进入点 (0,65,0)、白名单与权限表为空。 */
    @Test
    public void testDefaultValues() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);

        assertThat(entry.playerId).isEqualTo(OWNER);
        assertThat(entry.dimensionId).isEqualTo(Integer.MIN_VALUE);
        assertThat(entry.entryPoint).isEqualTo(new BlockPos(0, 65, 0));
        assertThat(entry.returnDim).isEqualTo(0);
        assertThat(entry.hasReturnPoint).isFalse();
        assertThat(entry.allowedPlayers).isEmpty();
        assertThat(entry.permissions).isEmpty();
    }

    /** 全字段 NBT 往返：维度 ID、进入点、返回点、白名单、权限表逐一相同。 */
    @Test
    public void testNbtRoundTrip() {
        PlayerDimEntry original = new PlayerDimEntry(OWNER);
        original.dimensionId = 42;
        original.rules.lockTime = true;
        original.rules.timeValue = 12345L;
        original.entryPoint = new BlockPos(-100, 70, 250);
        original.returnDim = -1;
        original.returnX = 1.5;
        original.returnY = 64.0;
        original.returnZ = -2.5;
        original.returnYaw = 90.0f;
        original.returnPitch = -30.0f;
        original.hasReturnPoint = true;
        original.grantPermission(GUEST_A, PersonalDimPermission.ENTER);
        original.grantPermission(GUEST_A, PersonalDimPermission.BUILD);
        original.grantPermission(GUEST_B, PersonalDimPermission.INTERACT);

        PlayerDimEntry restored = new PlayerDimEntry(OWNER);
        restored.readFromNBT(original.writeToNBT());

        assertThat(restored.dimensionId).isEqualTo(42);
        assertThat(restored.rules.lockTime).isTrue();
        assertThat(restored.rules.timeValue).isEqualTo(12345L);
        assertThat(restored.entryPoint).isEqualTo(new BlockPos(-100, 70, 250));
        assertThat(restored.returnDim).isEqualTo(-1);
        assertThat(restored.returnX).isEqualTo(1.5);
        assertThat(restored.returnY).isEqualTo(64.0);
        assertThat(restored.returnZ).isEqualTo(-2.5);
        assertThat(restored.returnYaw).isEqualTo(90.0f);
        assertThat(restored.returnPitch).isEqualTo(-30.0f);
        assertThat(restored.hasReturnPoint).isTrue();
        assertThat(restored.allowedPlayers).containsExactlyInAnyOrder(GUEST_A, GUEST_B);
        assertThat(restored.getPermissions(GUEST_A))
                .containsExactlyInAnyOrder(PersonalDimPermission.ENTER, PersonalDimPermission.BUILD);
        assertThat(restored.getPermissions(GUEST_B))
                .containsExactlyInAnyOrder(PersonalDimPermission.INTERACT);
    }

    /** entryPoint 的 BlockPos 通过 long 编解码，负坐标也能正确往返。 */
    @Test
    public void testEntryPointBlockPosRoundTripNegative() {
        PlayerDimEntry original = new PlayerDimEntry(OWNER);
        original.entryPoint = new BlockPos(-29999999, 5, 29999999);

        PlayerDimEntry restored = new PlayerDimEntry(OWNER);
        restored.readFromNBT(original.writeToNBT());

        assertThat(restored.entryPoint).isEqualTo(new BlockPos(-29999999, 5, 29999999));
    }

    /** 白名单反序列化容错：非法 UUID 字符串与空字符串被跳过，合法项保留。 */
    @Test
    public void testReadSkipsInvalidUuidInAllowedPlayers() {
        NBTTagCompound tag = baseTag();
        NBTTagList allowed = new NBTTagList();
        allowed.appendTag(uuidTag(GUEST_A.toString()));
        allowed.appendTag(uuidTag("not-a-uuid"));
        allowed.appendTag(uuidTag(""));
        tag.setTag("allowedPlayers", allowed);

        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.readFromNBT(tag);

        assertThat(entry.allowedPlayers).containsExactly(GUEST_A);
    }

    /** 权限表反序列化容错：未知权限名与空段被跳过，合法权限保留。 */
    @Test
    public void testReadSkipsUnknownPermissionNames() {
        NBTTagCompound tag = baseTag();
        NBTTagList perms = new NBTTagList();
        NBTTagCompound t = uuidTag(GUEST_A.toString());
        t.setString("permissions", "ENTER,BOGUS,,BUILD");
        perms.appendTag(t);
        tag.setTag("permissions", perms);

        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.readFromNBT(tag);

        assertThat(entry.getPermissions(GUEST_A))
                .containsExactlyInAnyOrder(PersonalDimPermission.ENTER, PersonalDimPermission.BUILD);
    }

    /** 权限表反序列化容错：UUID 非法的权限条目整条跳过，不影响其他条目。 */
    @Test
    public void testReadSkipsPermissionEntryWithInvalidUuid() {
        NBTTagCompound tag = baseTag();
        NBTTagList perms = new NBTTagList();
        NBTTagCompound bad = uuidTag("%%%");
        bad.setString("permissions", "ENTER");
        perms.appendTag(bad);
        NBTTagCompound good = uuidTag(GUEST_B.toString());
        good.setString("permissions", "ENTER");
        perms.appendTag(good);
        tag.setTag("permissions", perms);

        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.readFromNBT(tag);

        assertThat(entry.permissions).hasSize(1);
        assertThat(entry.hasPermission(GUEST_B, PersonalDimPermission.ENTER)).isTrue();
    }

    /** 权限字符串为空时该玩家仍会被放入权限表，但权限集合为空（当前实现行为固化）。 */
    @Test
    public void testReadEmptyPermissionStringYieldsEmptySetEntry() {
        NBTTagCompound tag = baseTag();
        NBTTagList perms = new NBTTagList();
        NBTTagCompound t = uuidTag(GUEST_A.toString());
        t.setString("permissions", "");
        perms.appendTag(t);
        tag.setTag("permissions", perms);

        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.readFromNBT(tag);

        assertThat(entry.permissions).containsKey(GUEST_A);
        assertThat(entry.getPermissions(GUEST_A)).isEmpty();
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.ENTER)).isFalse();
    }

    /** grantPermission 自动将玩家加入白名单，可重复授予多项权限。 */
    @Test
    public void testGrantPermissionAddsToWhitelist() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);

        entry.grantPermission(GUEST_A, PersonalDimPermission.ENTER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.BUILD);

        assertThat(entry.allowedPlayers).containsExactly(GUEST_A);
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.ENTER)).isTrue();
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.BUILD)).isTrue();
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.MANAGE_RULES)).isFalse();
    }

    /** revokePermission 移除单项权限后，玩家仍有其他权限时保留在白名单中。 */
    @Test
    public void testRevokeOneOfMultiplePermissionsKeepsPlayer() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.ENTER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.BUILD);

        entry.revokePermission(GUEST_A, PersonalDimPermission.BUILD);

        assertThat(entry.allowedPlayers).contains(GUEST_A);
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.ENTER)).isTrue();
        assertThat(entry.hasPermission(GUEST_A, PersonalDimPermission.BUILD)).isFalse();
    }

    /** revokePermission 清空该玩家全部权限后，自动清理权限表与白名单条目。 */
    @Test
    public void testRevokeLastPermissionCleansUpMapAndWhitelist() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.ENTER);

        entry.revokePermission(GUEST_A, PersonalDimPermission.ENTER);

        assertThat(entry.permissions).doesNotContainKey(GUEST_A);
        assertThat(entry.allowedPlayers).isEmpty();
    }

    /** 对不存在权限的玩家 revokePermission 是空操作，不抛异常。 */
    @Test
    public void testRevokeUnknownPlayerIsNoop() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.revokePermission(GUEST_A, PersonalDimPermission.ENTER);

        assertThat(entry.permissions).isEmpty();
        assertThat(entry.allowedPlayers).isEmpty();
    }

    /** removePlayer 将玩家从白名单与权限表中完全移除。 */
    @Test
    public void testRemovePlayer() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.ENTER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.MANAGE_RULES);

        entry.removePlayer(GUEST_A);

        assertThat(entry.allowedPlayers).isEmpty();
        assertThat(entry.permissions).isEmpty();
        assertThat(entry.getPermissions(GUEST_A)).isEmpty();
    }

    /** getPermissions 返回只读副本：尝试修改抛 UnsupportedOperationException，且副本不随后续授权变化。 */
    @Test
    public void testGetPermissionsReturnsReadOnlyCopy() {
        PlayerDimEntry entry = new PlayerDimEntry(OWNER);
        entry.grantPermission(GUEST_A, PersonalDimPermission.ENTER);

        java.util.Set<PersonalDimPermission> perms = entry.getPermissions(GUEST_A);
        assertThat(perms).containsExactly(PersonalDimPermission.ENTER);
        assertThatThrownBy(() -> perms.add(PersonalDimPermission.BUILD))
                .isInstanceOf(UnsupportedOperationException.class);

        // 副本独立性：之后再授权不影响先前拿到的副本
        entry.grantPermission(GUEST_A, PersonalDimPermission.BUILD);
        assertThat(perms).containsExactly(PersonalDimPermission.ENTER);
        assertThat(entry.getPermissions(GUEST_A))
                .containsExactlyInAnyOrder(PersonalDimPermission.ENTER, PersonalDimPermission.BUILD);

        // 未授权玩家返回空集合
        assertThat(entry.getPermissions(GUEST_B)).isEmpty();
    }

    /**
     * 版本兼容：readFromNBT(tag) 委托 readFromNBT(tag, 0)。
     * 当前实现中 version 参数未被使用（预留给未来字段扩展的向后兼容），
     * 因此 version=0 与 version=1 读出的结果完全相同。
     */
    @Test
    public void testVersionParameterCurrentlyIgnored() {
        PlayerDimEntry source = new PlayerDimEntry(OWNER);
        source.dimensionId = 7;
        source.entryPoint = new BlockPos(3, 64, -9);
        source.grantPermission(GUEST_A, PersonalDimPermission.ENTER);
        NBTTagCompound tag = source.writeToNBT();

        PlayerDimEntry v0 = new PlayerDimEntry(OWNER);
        v0.readFromNBT(tag, 0);
        PlayerDimEntry v1 = new PlayerDimEntry(OWNER);
        v1.readFromNBT(tag, 1);
        PlayerDimEntry vDefault = new PlayerDimEntry(OWNER);
        vDefault.readFromNBT(tag);

        for (PlayerDimEntry e : new PlayerDimEntry[]{v1, vDefault}) {
            assertThat(e.dimensionId).isEqualTo(v0.dimensionId);
            assertThat(e.entryPoint).isEqualTo(v0.entryPoint);
            assertThat(e.allowedPlayers).isEqualTo(v0.allowedPlayers);
            assertThat(e.permissions).isEqualTo(v0.permissions);
        }
    }

    /** 构造一个只含必填标量字段的基础 tag，便于手工追加白名单/权限表。 */
    private static NBTTagCompound baseTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("playerUUID", OWNER.toString());
        tag.setInteger("dimensionId", 5);
        tag.setTag("rules", new PersonalDimensionRules().writeToNBT());
        tag.setLong("entryPoint", new BlockPos(0, 65, 0).toLong());
        return tag;
    }

    private static NBTTagCompound uuidTag(String uuid) {
        NBTTagCompound t = new NBTTagCompound();
        t.setString("uuid", uuid);
        return t;
    }
}
