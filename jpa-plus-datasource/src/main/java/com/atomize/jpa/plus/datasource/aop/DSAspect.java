package com.atomize.jpa.plus.datasource.aop;

import com.atomize.jpa.plus.datasource.annotation.DS;
import com.atomize.jpa.plus.datasource.context.JpaPlusContext;
import com.atomize.jpa.plus.datasource.enums.DsName;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 数据源切换切面
 *
 * <p>拦截 {@link DS @DS} 注解，在方法执行前通过 {@link JpaPlusContext}
 * 创建 ScopedValue 作用域，方法结束后 <b>自动恢复到上层数据源（默认 master）</b>。</p>
 *
 * <h3>核心保证</h3>
 * <ul>
 *   <li>标注 {@code @DS("slave")} → 方法内所有 SQL 走 slave</li>
 *   <li>方法执行完毕（正常或异常）→ 自动退出 ScopedValue 作用域 → 恢复为 master</li>
 *   <li>嵌套调用：{@code @DS("slave")} 内部调用 {@code @DS("analytics")} → analytics 生效 → 返回后恢复 slave → 最终恢复 master</li>
 *   <li>不加 {@code @DS} 的方法 → 不经过此切面 → 始终走默认 master</li>
 * </ul>
 *
 * <p><b>为什么不会"粘连到从库"？</b><br/>
 * {@link ScopedValue} 是块作用域的（block-scoped），
 * 与 ThreadLocal 不同，它不需要手动 remove()，
 * 离开 {@code ScopedValue.where().call()} 块后自动回到外层绑定值（或无绑定 → master）。</p>
 *
 * <h3>优先级</h3>
 * <p>方法级 {@code @DS} 优先于类级 {@code @DS}。</p>
 *
 * @author guanxiangkai
 * @see DS
 * @see JpaPlusContext
 * @since 2026年03月25日 星期三
 */
@Slf4j
@Aspect
public class DSAspect {

    /**
     * 拦截所有标注 {@code @DS} 的方法和类中的方法
     */
    @Around("@annotation(com.atomize.jpa.plus.datasource.annotation.DS) || " +
            "@within(com.atomize.jpa.plus.datasource.annotation.DS)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        String dsName = resolveDS(point);

        if (log.isDebugEnabled()) {
            log.debug("@DS switch: {} → {}", point.getSignature().toShortString(), dsName);
        }

        // ScopedValue.where().call() 创建一个块作用域：
        // - 进入块：CURRENT_DS 绑定为 dsName
        // - 退出块（正常/异常）：CURRENT_DS 自动恢复为外层绑定值（无外层则回到 unbound → master）
        return JpaPlusContext.withDS(dsName, (JpaPlusContext.ThrowableCallable<Object>) point::proceed);
    }

    /**
     * 解析数据源名称（方法级优先于类级）
     */
    private String resolveDS(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // ① 方法级 @DS（最高优先级）
        DS methodDS = method.getAnnotation(DS.class);
        if (methodDS != null) {
            return methodDS.value();
        }

        // ② 类级 @DS
        DS classDS = point.getTarget().getClass().getAnnotation(DS.class);
        if (classDS != null) {
            return classDS.value();
        }

        // ③ 无注解 → master（理论上不会走到这里，因为切点已限定）
        return DsName.MASTER;
    }
}

