import JSEncrypt from 'jsencrypt';
// 密钥对生成 http://web.chacuo.net/netrsakeypair

const publicKey = import.meta.env.VITE_APP_RSA_PUBLIC_KEY;

// 加密 — 请求体加密用公钥，公钥放前端是预期的。
export const encrypt = (txt: string) => {
  const encryptor = new JSEncrypt();
  encryptor.setPublicKey(publicKey); // 设置公钥
  return encryptor.encrypt(txt); // 对数据进行加密
};

// 解密接口已彻底移除：前端任何 dist 都会被反编译出 import.meta.env.*，
// RSA 私钥放在前端等同于把私钥公开。响应解密统一移到后端 / BFF 完成，
// 前端如需读取密文响应，请改由后端先解密再下发明文。

