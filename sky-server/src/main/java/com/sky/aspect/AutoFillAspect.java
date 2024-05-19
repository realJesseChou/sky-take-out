package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自动填充切面
 */
@Aspect
@Slf4j
@Component
public class AutoFillAspect {
    /**
     * 切入点
     * */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    /**
     * 前置通知，在修改方法执行前为公共字段赋值。
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint){
        log.info("对公共字段进行填充");
        // 1.获取当前被拦截方法的数据库操作类型
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();      // 获取方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);    // 获取方法上的注解
        OperationType operationType = autoFill.value();                     // 获取注解中的操作类型

        // 2.获取当前被拦截方法的参数
        Object[] args = joinPoint.getArgs();    // 获取方法所有参数
        Object obj = args[0];

        // 3.准备要赋的值
        LocalDateTime now = LocalDateTime.now();
        Long curUsrId = BaseContext.getCurrentId();

        // 4.根据数据库操作类型，为指定参数赋值
        if(operationType == OperationType.INSERT) {
            // 对于插入操作需要用反射为四个公共字段赋值

            try {
                // 获取方法
                Method setCreateTime =  obj.getClass().getMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = obj.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = obj.getClass().getMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = obj.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 反射赋值
                setCreateTime.invoke(obj, now);
                setUpdateTime.invoke(obj, now);
                setCreateUser.invoke(obj, curUsrId);
                setUpdateUser.invoke(obj, curUsrId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }else if(operationType == OperationType.UPDATE){
            // 对于更新操作需要用反射为两个公共字段赋值
            try {
                // 获取方法
                Method setUpdateTime = obj.getClass().getMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = obj.getClass().getMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 反射赋值
                setUpdateTime.invoke(obj, now);
                setUpdateUser.invoke(obj, curUsrId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
