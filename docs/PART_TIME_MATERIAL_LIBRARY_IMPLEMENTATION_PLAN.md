# 兼职任务素材库实施计划

> 文档版本：V1.0  
> 编写日期：2026-06-23  
> 对应 PRD：[[PART_TIME_MATERIAL_LIBRARY_PRD]]  
> 技术栈：RuoYi-Vue-Plus + Vue 3 + UniApp + MySQL + MinIO  
> 当前优先级：P0

---

## 1. 实施目标

本阶段完成“后台素材库 -> 任务绑定分类 -> 小程序领取自动分配 -> 作品审核可追踪”的闭环。

最终效果：

```text
后台维护文案和图片
-> 发布任务时选择文案分类和图片分类
-> 小程序用户领取任务
-> 系统按顺序分配文案和图片
-> 用户复制文案、保存图片、发布抖音
-> 用户提交作品链接
-> 后台审核时查看分配素材
```

---

## 2. 总体阶段

| 阶段 | 名称 | 目标 |
| --- | --- | --- |
| M1 | 数据库与菜单权限 | 建表、菜单、权限标识 |
| M2 | 后端素材库接口 | 分类、文案、图片 CRUD |
| M3 | 后台管理页面 | 素材库页面和任务发布绑定分类 |
| M4 | 领取任务分配逻辑 | 小程序领取时自动分配素材 |
| M5 | 小程序展示素材 | 复制文案、查看图片、保存图片 |
| M6 | 审核页联动 | 审核作品时展示分配素材 |
| M7 | 测试与修复 | 验证循环分配、并发、异常场景 |

---

## 3. M1：数据库与菜单权限

### 3.1 SQL 脚本

新增 SQL 文件建议：

```text
sql/parttime_material_library_schema.sql
```

需要创建：

- `pt_material_category`
- `pt_material_text`
- `pt_material_image`
- `pt_task_material_config`
- `pt_task_material_assignment`

### 3.2 索引和约束

建议索引：

```text
pt_material_category:
- idx_tenant_type_status(tenant_id, category_type, status)

pt_material_text:
- idx_category_status_sort(category_id, status, sort, id)

pt_material_image:
- idx_category_status_sort(category_id, status, sort, id)

pt_task_material_config:
- uk_task_id(task_id)

pt_task_material_assignment:
- uk_claim_id(claim_id)
- idx_task_user(task_id, user_id)
- idx_task_assign(task_id, assign_index)
```

### 3.3 菜单规划

后台菜单建议放在：

```text
兼职任务管理
  - 兼职人员
  - 兼职任务
  - 作品审核
  - 素材库
      - 素材分类
      - 文案库
      - 图片库
```

如果为了第一期更快，可以先做成：

```text
兼职任务管理
  - 素材分类
  - 文案库
  - 图片库
```

### 3.4 权限标识

```text
parttime:material:category:list
parttime:material:category:add
parttime:material:category:edit
parttime:material:category:remove

parttime:material:text:list
parttime:material:text:add
parttime:material:text:edit
parttime:material:text:remove

parttime:material:image:list
parttime:material:image:add
parttime:material:image:edit
parttime:material:image:remove
```

### 3.5 完成标准

- [ ] SQL 可重复确认执行。
- [ ] 表结构带租户字段、创建时间、更新时间、删除标记。
- [ ] 菜单权限可分配给管理员角色。
- [ ] 表索引覆盖分类查询、素材查询、领取分配查询。

---

## 4. M2：后端素材库接口

### 4.1 后端目录建议

继续放在 creator 模块：

```text
server/ruoyi-modules/ruoyi-creator/src/main/java/org/dromara/creator
```

新增领域对象：

```text
domain/PtMaterialCategory.java
domain/PtMaterialText.java
domain/PtMaterialImage.java
domain/PtTaskMaterialConfig.java
domain/PtTaskMaterialAssignment.java
```

新增 BO/VO：

```text
domain/bo/PtMaterialCategoryBo.java
domain/bo/PtMaterialTextBo.java
domain/bo/PtMaterialImageBo.java
domain/vo/PtMaterialCategoryVo.java
domain/vo/PtMaterialTextVo.java
domain/vo/PtMaterialImageVo.java
```

新增 Mapper/Service/Controller：

```text
mapper/PtMaterialCategoryMapper.java
mapper/PtMaterialTextMapper.java
mapper/PtMaterialImageMapper.java

service/IPtMaterialCategoryService.java
service/IPtMaterialTextService.java
service/IPtMaterialImageService.java

service/impl/PtMaterialCategoryServiceImpl.java
service/impl/PtMaterialTextServiceImpl.java
service/impl/PtMaterialImageServiceImpl.java

controller/PtMaterialCategoryController.java
controller/PtMaterialTextController.java
controller/PtMaterialImageController.java
```

### 4.2 后台接口

分类：

```text
GET    /parttime/material/category/list
GET    /parttime/material/category/{id}
POST   /parttime/material/category
PUT    /parttime/material/category
DELETE /parttime/material/category/{ids}
GET    /parttime/material/category/options?type=text|image
```

文案：

```text
GET    /parttime/material/text/list
GET    /parttime/material/text/{id}
POST   /parttime/material/text
PUT    /parttime/material/text
DELETE /parttime/material/text/{ids}
```

图片：

```text
GET    /parttime/material/image/list
GET    /parttime/material/image/{id}
POST   /parttime/material/image
PUT    /parttime/material/image
DELETE /parttime/material/image/{ids}
```

图片上传可以优先复用若依现有文件上传能力，素材图片表只保存返回的 URL、文件名和大小。

### 4.3 服务规则

- 删除已被分配使用的素材时，优先转为停用或软删除。
- `options` 接口只返回启用分类。
- 任务发布前调用素材数量校验。
- 分类类型必须和素材类型匹配。

### 4.4 完成标准

- [ ] 分类 CRUD 正常。
- [ ] 文案 CRUD 正常。
- [ ] 图片 CRUD 正常。
- [ ] 上传图片后可以保存到图片库。
- [ ] 已停用素材不参与后续分配。
- [ ] 后端接口有权限注解。

---

## 5. M3：后台管理页面

### 5.1 前端页面目录建议

```text
admin/src/views/parttime/material/category/index.vue
admin/src/views/parttime/material/text/index.vue
admin/src/views/parttime/material/image/index.vue
```

具体路径以当前后台项目实际目录为准。

### 5.2 素材分类页面

功能：

- 列表查询
- 新增分类
- 编辑分类
- 启用 / 停用
- 删除
- 按类型筛选

字段：

- 分类名称
- 类型
- 状态
- 排序
- 备注
- 创建时间

### 5.3 文案库页面

功能：

- 按分类筛选
- 新增文案
- 编辑文案
- 启用 / 停用
- 删除
- 文案内容预览

字段：

- 所属分类
- 文案内容
- 状态
- 排序
- 创建时间

### 5.4 图片库页面

功能：

- 按分类筛选
- 上传图片
- 图片预览
- 编辑图片信息
- 启用 / 停用
- 删除

字段：

- 所属分类
- 图片缩略图
- 图片名称
- 状态
- 排序
- 创建时间

### 5.5 任务发布页改造

在兼职任务新增/编辑表单中增加：

- 文案分类选择
- 图片分类选择

规则：

- 发布任务前校验分类下是否有启用素材。
- 草稿状态可以修改分类。
- 已发布任务修改分类时提示“只影响后续领取用户”。

### 5.6 完成标准

- [ ] 管理员能在后台维护分类。
- [ ] 管理员能在后台维护文案。
- [ ] 管理员能在后台维护图片。
- [ ] 创建任务时能选择文案分类和图片分类。
- [ ] 发布任务前能阻止空分类。

---

## 6. M4：领取任务分配逻辑

### 6.1 需要改造的后端能力

重点改造领取任务接口。

可能涉及：

```text
MiniappTaskController
ParttimeTaskController
PtTaskClaimService
PtTaskService
```

具体以当前代码实际命名为准。

### 6.2 领取事务

领取任务时需要在同一个事务内完成：

1. 校验用户资料状态是否允许领取。
2. 校验任务状态、名额、截止时间。
3. 校验用户是否已经领取。
4. 创建或返回领取记录。
5. 计算领取序号 `assign_index`。
6. 查询任务绑定的文案分类和图片分类。
7. 查询可用文案和可用图片。
8. 根据顺序循环规则选中文案和图片。
9. 保存素材分配记录和快照。

### 6.3 分配服务建议

新增服务：

```text
IPtTaskMaterialAssignmentService
PtTaskMaterialAssignmentServiceImpl
```

核心方法：

```text
assignForClaim(taskId, claimId, userId)
getByClaimId(claimId)
```

### 6.4 分配规则

```text
assign_index = 当前任务第 N 个成功领取记录

text_position = (assign_index - 1) % enabled_text_count
image_position = (assign_index - 1) % enabled_image_count
```

### 6.5 完成标准

- [ ] 用户领取任务后自动生成素材分配记录。
- [ ] 同一用户重复点击领取不会重复生成分配。
- [ ] 文案和图片按顺序循环。
- [ ] 分类素材为空时领取失败并给出明确提示。
- [ ] 领取记录和分配记录不会出现一边成功一边失败。

---

## 7. M5：小程序展示素材

### 7.1 页面改造

重点页面：

```text
miniapp/src/pages/tasks/index.vue
miniapp/src/pages/works/submit.vue
miniapp/src/pages/works/index.vue
```

具体根据当前页面结构调整。

### 7.2 任务详情或提交页展示

领取后展示：

- 分配文案卡片
- 复制文案按钮
- 分配图片卡片
- 图片预览
- 保存图片按钮
- 发布步骤说明
- 提交作品链接表单

### 7.3 小程序交互规则

- 点击复制文案：调用 `uni.setClipboardData`。
- 点击保存图片：优先调用 `uni.saveImageToPhotosAlbum`。
- 保存失败时提示用户检查相册权限。
- 如果图片 URL 在本地开发无法保存，可以先支持预览。

### 7.4 完成标准

- [ ] 用户领取后能看到文案。
- [ ] 用户可以一键复制文案。
- [ ] 用户领取后能看到图片。
- [ ] 用户可以预览图片。
- [ ] 用户可以保存图片或看到明确失败提示。
- [ ] 用户提交作品时关联当前领取记录。

---

## 8. M6：作品审核联动

### 8.1 后台审核列表

作品审核列表增加：

- 分配文案摘要
- 分配图片缩略图

如果列表太挤，可以只在详情弹窗展示完整内容。

### 8.2 审核详情

建议新增审核详情弹窗，展示：

- 任务信息
- 兼职人员信息
- 用户提交作品链接
- 用户提交作品文案
- 系统分配文案
- 系统分配图片
- 审核操作

### 8.3 完成标准

- [ ] 审核人员能看到用户被分配的文案。
- [ ] 审核人员能看到用户被分配的图片。
- [ ] 审核通过后原有内容监测逻辑不受影响。
- [ ] 驳回后保留素材分配记录。

---

## 9. M7：测试清单

### 9.1 基础测试

- [ ] 新增文案分类。
- [ ] 新增图片分类。
- [ ] 新增 3 条文案。
- [ ] 上传 5 张图片。
- [ ] 创建任务并绑定两个分类。
- [ ] 发布任务成功。

### 9.2 分配测试

准备：

- 文案 3 条
- 图片 5 张
- 测试用户 6 个

预期：

| 用户 | 文案 | 图片 |
| --- | --- | --- |
| 用户1 | 文案1 | 图片1 |
| 用户2 | 文案2 | 图片2 |
| 用户3 | 文案3 | 图片3 |
| 用户4 | 文案1 | 图片4 |
| 用户5 | 文案2 | 图片5 |
| 用户6 | 文案3 | 图片1 |

### 9.3 重复领取测试

- [ ] 同一用户连续点击领取 3 次，只产生 1 条领取记录。
- [ ] 同一用户重复进入任务详情，素材不变化。
- [ ] 用户提交作品后，素材分配仍可查询。

### 9.4 异常测试

- [ ] 文案分类为空，任务不能发布。
- [ ] 图片分类为空，任务不能发布。
- [ ] 任务发布后素材全部停用，新用户不能领取。
- [ ] 任务已截止，不能领取。
- [ ] 用户资料未审核通过，不能领取。
- [ ] 图片保存失败时，小程序有明确提示。

### 9.5 回归测试

- [ ] 原有兼职任务列表正常。
- [ ] 原有任务领取逻辑正常。
- [ ] 原有作品提交逻辑正常。
- [ ] 原有作品审核通过后进入内容监测逻辑正常。
- [ ] 抖音内容监测页面不受影响。

---

## 10. 开发顺序建议

建议按这个顺序执行：

1. 建表和菜单权限。
2. 后端素材分类 CRUD。
3. 后端文案库 CRUD。
4. 后端图片库 CRUD。
5. 后台素材库页面。
6. 兼职任务表单增加素材分类选择。
7. 任务发布校验素材数量。
8. 领取任务时生成素材分配。
9. 小程序任务详情展示素材。
10. 作品审核页展示素材。
11. 完整走通一条真实测试流程。

---

## 11. 风险和注意事项

### 11.1 领取分配并发

风险：

多个用户同时领取同一任务时，如果领取序号计算不严谨，可能出现重复分配序号。

处理：

- 领取和分配放进同一个事务。
- 对任务领取计数或任务行加锁。
- 对用户领取建立唯一约束。

### 11.2 已分配素材被修改

风险：

工作人员修改文案后，历史用户看到的素材变化，审核无法追踪。

处理：

- 分配表保存文案和图片快照。
- 审核页优先展示分配快照。

### 11.3 小程序保存图片限制

风险：

本地开发或非 HTTPS 图片地址可能不能保存到相册。

处理：

- 本地先支持图片预览。
- 正式上线前保证图片 URL 为合法 HTTPS 域名。

### 11.4 素材库和任务强绑定

风险：

任务发布后分类被停用或素材被清空，后续用户无法领取。

处理：

- 修改素材状态时提示影响范围。
- 领取时再次校验可用素材。
- 已领取用户使用快照，不受影响。

---

## 12. 第一轮最小实现范围

为了最快跑通，第一轮只做这些：

- [ ] 文案分类 CRUD。
- [ ] 图片分类 CRUD。
- [ ] 文案 CRUD。
- [ ] 图片上传和列表。
- [ ] 任务绑定文案分类和图片分类。
- [ ] 领取时顺序循环分配。
- [ ] 小程序展示分配文案和图片。
- [ ] 审核页展示分配文案和图片。

暂时不做：

- [ ] 素材效果统计。
- [ ] 素材批量导入。
- [ ] 素材使用次数看板。
- [ ] AI 生成。
- [ ] 自动识别用户是否使用指定素材。

---

## 13. 当前下一步

下一步建议直接进入 M1：

1. 先写 `sql/parttime_material_library_schema.sql`。
2. 创建 5 张表和基础菜单权限。
3. 再开始生成后端 Domain、Mapper、Service、Controller。

完成 M1 后，再进入后台素材库页面开发。
