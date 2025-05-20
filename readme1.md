# 项目情况
- Java，kotlin混合开发



## 开发规范
- 使用kotlin
- 全面使用AndroidX，Android KTX库
- Kotlin Flow 替代 RxJava
- DataStore 替代 SharedPreferences
- 采用 MVVM 架构模式
- 使用 ViewModel 和 LiveData/Flow
- 日志使用timber.log.Timber
- 使用Material Design 3
- 不允许使用弃用的api，除非没有替代方法或者刚需
- 

## 功能实现
- 使用github api 实现app检查更新
- 优化侧边栏
- 优化主页

## bug记录


## 开发进度

### MyScriptListFragment迁移
- [x] 创建Kotlin版本Fragment
- [x] 移除FAB依赖，改为标题栏菜单
- [x] 迁移权限逻辑到Fragment
- [ ] 集成Navigation组件
- [ ] 测试验证

### 整体迁移计划
- [ ] MyScriptListFragment迁移完成
- [ ] DocsFragment迁移完成
- [ ] TaskManagerFragment迁移完成
- [ ] FragmentPagerAdapterBuilder迁移到Navigation
- [ ] MainActivity迁移到BaseActivityV2