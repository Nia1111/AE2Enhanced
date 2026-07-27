package team.chisel.ctm.api;

/**
 * 测试桩:AE2-UEL 的块定义类实现了 CTM(ConnectedTexturesMod)的可选接口,
 * 测试环境没有该 mod,提供空接口满足类链接.方法在 Api 初始化路径上不会被调用,
 * 若后续出现 NoSuchMethodError 再按需补充签名.
 */
public interface IFacade {
}
