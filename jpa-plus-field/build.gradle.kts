/**
 * jpa-plus-field — 字段治理（合并模块）
 *
 * <p>聚合所有 FieldHandler 实现：
 * <ul>
 *   <li>ID 自动生成（@AutoId → IdFieldHandler）</li>
 *   <li>自动填充（@CreateTime/@UpdateTime/@CreateBy/@UpdateBy → AutoFillFieldHandler）</li>
 *   <li>乐观锁（@Version → VersionFieldHandler）</li>
 *   <li>字段加密（@Encrypt → EncryptFieldHandler）</li>
 *   <li>字段脱敏（@Desensitize → DesensitizeFieldHandler）</li>
 *   <li>字典回写（@Dict → DictFieldHandler）</li>
 *   <li>敏感词检测（@SensitiveWord → SensitiveWordHandler）</li>
 * </ul>
 * </p>
 */
dependencies {
    api(project(":jpa-plus-core"))
}

