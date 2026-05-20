<template>
  <router-view />
</template>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f7;
}

:root {
  --primary: #409eff;
  --success: #67c23a;
  --warning: #e6a23c;
  --danger: #f56c6c;
  --purple: #9c59f7;
  --card-radius: 14px;
  --card-shadow: 0 2px 16px rgba(0,0,0,0.07);
  --card-shadow-hover: 0 8px 32px rgba(0,0,0,0.13);
}

/* 卡片统一圆角 */
.el-card { border-radius: var(--card-radius) !important; border: none !important; box-shadow: var(--card-shadow) !important; }
.el-card:hover { box-shadow: var(--card-shadow-hover) !important; transition: box-shadow 0.25s; }
.el-card__header { border-bottom: 1px solid #f5f5f5 !important; font-weight: 600; color: #1a1a2e; padding: 14px 20px !important; }
.el-card__body { padding: 20px !important; }

/* 表格美化 */
.el-table { border-radius: 10px; overflow: hidden; }
.el-table th.el-table__cell { background: #f8f9fc !important; color: #555; font-weight: 600; font-size: 13px; }
.el-table__row:hover > td { background: #f0f5ff !important; }

/* 按钮 */
.el-button--primary { background: linear-gradient(135deg, #409eff, #5b8def) !important; border: none !important; }
.el-button--success { background: linear-gradient(135deg, #67c23a, #52c41a) !important; border: none !important; }

/* Tag美化 */
.el-tag { border-radius: 20px !important; font-size: 12px !important; }

/* 分页 */
.el-pagination { padding: 12px 0 0; }

/* 表单label */
.el-form-item__label { color: #555; font-size: 13px; font-weight: 500; }

/* 输入框 */
.el-input__wrapper, .el-select__wrapper { border-radius: 8px !important; }

/* 移动端响应 */
@media (max-width: 767px) {
  .el-card__body { padding: 14px !important; }
  .el-table { font-size: 12px; }
  .el-form--inline .el-form-item { margin-right: 8px; margin-bottom: 8px; }
  .el-pagination { justify-content: center; flex-wrap: wrap; }
  :root { --card-radius: 10px; }
}
</style>
