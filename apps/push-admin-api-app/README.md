*可部署应用*

面向管理者、运营、客服、内部支持人员。

职责：

- 模板管理
- 渠道账号管理
- 人工待确认处理
- 链路追踪和内部查询
- 后续的报表查询、补偿触发


部署特点：

- 内网部署
- 和用户侧接口彻底隔离
- 强制接入统一认证鉴权
- 副本数可少于 public-api，但必须有审计日志

生产部署建议：

- 2 个副本即可
- 只开放内网或 VPN
- 强制接入统一登录、RBAC、操作审计
- 上传文件、人群包导入、模板审批都在这里

来源映射：

MessageTemplateController
ChannelAccountController
PendingConfirmController
DataTraceController