# SSH 密钥与服务器对照（本地记录，勿提交私钥）

> `cert/` 目录已在 `.gitignore` 中，**禁止**将 `.pem` 提交到 Git。

| 密钥文件 | 对应服务器 | SSH 用户 | 验证状态 | 备注 |
|----------|------------|----------|:--------:|------|
| **im1.pem** | **18.139.209.10**（新加坡 AWS） | **ec2-user** | ✅ 已验证 | 宝塔面板机；hostname `ip-172-31-30-116` |
| imback.pem | 未知 | — | ❌ 非本机 | 对 18.139.209.10 无效 |
| MQ1.pem | 未知 | — | ❌ 非本机 | 可能为 RabbitMQ 专用机 |
| MySQL1.pem | 未知 | — | ❌ 非本机 | 可能为 MySQL 专用机 |
| redis1.pem | 未知 | — | ❌ 非本机 | 可能为 Redis 专用机 |
| xinfuwuqi.pem | 未知 | — | ❌ 非本机 | 名称暗示「新服务器」 |

## 连接示例（Windows 需用 Python/paramiko 或修复 PEM 权限）

```bash
# Paramiko（推荐，绕过 Windows PEM 权限问题）
python deploy/scripts/server_audit_quick.py --host 18.139.209.10 --user ec2-user --key-file cert/im1.pem
```

## 18.139.209.10 扫描日期

2026-06-25 — 见下方 AGENT/会话结论或重新跑 `server_audit_quick.py`
