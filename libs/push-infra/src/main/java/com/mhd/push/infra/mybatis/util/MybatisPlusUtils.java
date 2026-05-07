package com.mhd.push.infra.mybatis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mhd.push.common.utils.CollectionUtils;
import com.mhd.push.infra.mybatis.domain.PageParam;
import com.mhd.push.infra.mybatis.domain.SortableParam;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MyBatis-Plus 常用工具类
 * 提供常用的数据库操作封装，简化业务代码
 *
 * @author zhao-hao-dong
 **/
@Slf4j
public final class MybatisPlusUtils {
    private MybatisPlusUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 批量删除时的单次操作最大数量阈值
     * 当删除数据量超过此阈值时，将使用分批删除策略
     */
    private static final int BATCH_DELETE_THRESHOLD = 5000;

    /**
     * 分批删除时的每批次大小
     */
    private static final int BATCH_SIZE = 5000;

    ///**
    // * 新增时验证字段值唯一性
    // * 如果值已存在则抛出业务异常
    // *
    // * @param service 服务层对象
    // * @param field   要验证的字段
    // * @param value   要验证的值
    // * @param <S>     服务类型
    // * @param <T>     实体类型
    // * @throws BusinessException 当字段值已存在时抛出
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfAdd(S service, SFunction<T, ?> field, Object value) {
    //    validColumnValueIfExistOfAdd(service, field, value, null, null);
    //}
    //
    ///**
    // * 新增时验证字段值唯一性
    // * 如果值已存在则抛出自定义异常信息
    // *
    // * @param service 服务层对象
    // * @param field   要验证的字段
    // * @param value   要验证的值
    // * @param message 自定义异常信息
    // * @param <S>     服务类型
    // * @param <T>     实体类型
    // * @throws BusinessException 当字段值已存在时抛出
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfAdd(S service, SFunction<T, ?> field, Object value, String message) {
    //    validColumnValueIfExistOfAdd(service, field, value, null, message);
    //}
    //
    ///**
    // * 新增时验证字段值唯一性（支持自定义过滤条件）
    // *
    // * @param service   服务层对象
    // * @param field     要验证的字段
    // * @param value     要验证的值
    // * @param predicate 额外的过滤条件
    // * @param message   自定义异常信息
    // * @param <S>       服务类型
    // * @param <T>       实体类型
    // * @throws BusinessException 当字段值已存在时抛出
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfAdd(S service, SFunction<T, ?> field, Object value, Predicate<? super T> predicate, String message) {
    //    if (field == null || ObjectUtil.isEmpty(value)) {
    //        return;
    //    }
    //    List<T> existingList = service.list(new LambdaQueryWrapperX<T>().eq(field, value));
    //    if (CollUtil.isEmpty(existingList)) {
    //        return;
    //    }
    //
    //    Predicate<? super T> finalPredicate = predicate != null ? predicate : t -> true;
    //    boolean exists = existingList.stream().anyMatch(finalPredicate);
    //    if (exists) {
    //        String errorMessage = StrUtil.isNotBlank(message) ? message : "该字段值已存在，请重新输入";
    //        log.warn("新增时唯一性验证失败：字段值[{}]已存在", value);
    //        throw new BusinessException(ErrorCodeEnum.ADD_ERROR, errorMessage);
    //    }
    //
    //}
    //
    ///**
    // * 更新时验证字段值唯一性
    // *
    // * @param service 服务层对象
    // * @param field   要验证的字段
    // * @param value   要验证的值
    // * @param <S>     服务类型
    // * @param <T>     实体类型
    // * @throws BusinessException 当字段值已存在时抛出
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfUpdate(S service, SFunction<T, ?> field, Object value) {
    //    MybatisPlusUtils.validColumnValueIfExistOfUpdate(service, field, value, null, null);
    //}
    //
    ///**
    // * 更新时验证字段值唯一性（支持自定义异常信息）
    // *
    // * @param service 服务层对象
    // * @param field   要验证的字段
    // * @param value   要验证的值
    // * @param message 自定义异常信息
    // * @param <S>     服务类型
    // * @param <T>     实体类型
    // * @throws BusinessException 当字段值已存在时抛出
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfUpdate(S service, SFunction<T, ?> field, Object value, String message) {
    //    MybatisPlusUtils.validColumnValueIfExistOfUpdate(service, field, value, null, message);
    //}
    //
    ///**
    // * 在更新时校验某一列的值是否存在，如果存在则抛出异常
    // *
    // * @param service   service
    // * @param field     校验的字段
    // * @param value     校验的值
    // * @param predicate 自定义过滤条件
    // * @param message   异常信息
    // */
    //public static <S extends IService<T>, T> void validColumnValueIfExistOfUpdate(S service, SFunction<T, ?> field, Object value, Predicate<? super T> predicate, String message) {
    //    if (field == null || ObjectUtil.isEmpty(value)) {
    //        return;
    //    }
    //    List<T> existingList = service.list(new LambdaQueryWrapperX<T>().eq(field, value));
    //    if (CollUtil.isEmpty(existingList)) {
    //        return;
    //    }
    //    Predicate<? super T> finalPredicate = predicate != null ? predicate : t -> true;
    //    boolean exists = existingList.stream().anyMatch(finalPredicate);
    //    if (exists) {
    //        String errorMessage = StrUtil.isNotBlank(message) ? message : "该字段值已存在，请重新输入";
    //        log.warn("修改时唯一性验证失败：字段值[{}]已存在", value);
    //        throw new BusinessException(ErrorCodeEnum.UPDATE_ERROR, errorMessage);
    //    }
    //}
    //
    ///**
    // * 根据某一列的值查询数据
    // *
    // * @param service 查询的service接口层
    // * @param field   字段field
    // * @param value   值value
    // * @return 返回list
    // */
    //public static <S extends IService<T>, T> List<T> selectListByColumn(S service, SFunction<T, ?> field, Object value) {
    //    if (field == null || ObjectUtil.isEmpty(value)) {
    //        return Collections.emptyList();
    //    }
    //    return service.list(new LambdaQueryWrapperX<T>().eq(field, value));
    //}
    //
    ///**
    // * 根据某一列的值查询数据
    // *
    // * @param service   查询的service接口层
    // * @param field     字段field
    // * @param valueList 值value
    // * @return 返回list
    // */
    //public static <S extends IService<T>, T> List<T> selectListByColumnList(S service, SFunction<T, ?> field, Collection<?> valueList) {
    //    if (field == null || CollUtil.isEmpty(valueList)) {
    //        return Collections.emptyList();
    //    }
    //    return service.list(new LambdaQueryWrapperX<T>().in(field, valueList));
    //}
    //
    ///**
    // * 根据某一列的值删除数据
    // *
    // * @param service 查询的service接口层
    // * @param field   字段field
    // * @param value   值value
    // * @return 返回list
    // */
    //public static <S extends IService<T>, T> boolean deleteByColumn(S service, SFunction<T, ?> field, Object value) {
    //    if (field == null || ObjectUtil.isEmpty(value)) {
    //        return true;
    //    }
    //    return service.remove(new LambdaQueryWrapperX<T>().eq(field, value));
    //}

    /**
     * 根据某一列的值删除数据
     *
     * @param service   查询的service接口层
     * @param field     字段field
     * @param valueList 值value
     * @return 返回list
     */
    public static <S extends IService<T>, T> boolean deleteByColumnList(S service, SFunction<T, ?> field, Collection<?> valueList) {
        if (field == null || CollUtil.isEmpty(valueList)) {
            return true;
        }
        // 查询实际要删除的数据总量
        long totalCount = valueList.size();
        String svc = service.getClass().getSimpleName();
        // 根据数据量选择删除策略
        if (totalCount <= BATCH_DELETE_THRESHOLD) {
            service.remove(new LambdaQueryWrapper<T>().in(field, valueList));
            log.info("{} batch-deleted {} rows", svc, totalCount);
        } else {
            deleteByColumnListInBatches(service, field, valueList);
        }
        return true;
    }

    /**
     * 分批删除数据的内部方法
     * 直接基于条件删除，适用于无主键或主键复杂的场景
     *
     * @param service   查询的service接口层
     * @param field     字段field
     * @param valueList 值集合
     * @param <S>       Service泛型
     * @param <T>       实体泛型
     */
    private static <S extends IService<T>, T> void deleteByColumnListInBatches(S service, SFunction<T, ?> field, Collection<?> valueList) {
        int totalDeleted = 0;
        String svc = service.getClass().getSimpleName();
        List<?> list = new ArrayList<>(valueList);
        int total = list.size();
        int totalBatch = (total + BATCH_SIZE - 1) / BATCH_SIZE;
        for (int batchNum = 1; batchNum <= totalBatch; batchNum++) {
            int from = (batchNum - 1) * BATCH_SIZE;
            int to = Math.min(from + BATCH_SIZE, total);
            List<?> sub = list.subList(from, to);
            LambdaQueryWrapper<T> queryWrapper = new LambdaQueryWrapper<T>()
                    .in(field, sub);
            // 查询当前批次数据
            List<T> batchData = service.list(queryWrapper);
            if (CollUtil.isEmpty(batchData)) {
                continue;
            }
            // 直接基于条件删除，避免主键依赖
            boolean success = service.remove(queryWrapper);
            if (success) {
                totalDeleted += batchData.size();
                log.debug("{} batch {} removed {} rows (total {})", svc, batchNum, batchData.size(), totalDeleted);
            } else {
                log.warn("{} batch {} failed", svc, batchNum);
            }
        }
        log.info("{} finished batch-delete, {} rows removed", svc, totalDeleted);
    }

    public static <T> Page<T> buildPage(PageParam pageParam, SortableParam sortableParam) {
        if (pageParam == null) {
            throw new IllegalArgumentException("分页参数不能为空");
        }
        // 创建分页对象
        Page<T> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        // 添加排序条件
        if (sortableParam != null) {
            List<OrderItem> orderItems = buildOrderItems(sortableParam);
            if (CollUtil.isNotEmpty(orderItems)) {
                page.addOrder(orderItems);
            }
        }
        return page;
    }

    public static List<OrderItem> buildOrderItems(SortableParam sortableParam) {
        if (sortableParam == null || CollUtil.isEmpty(sortableParam.getSortingFields())) {

            return Collections.emptyList();
        }
        List<String> sortingFields = sortableParam.getSortingFields();
        return CollectionUtils.convertList(sortingFields, sortingField -> {
            if (StrUtil.isBlank(sortingField)) {
                log.warn("排序字段为空，跳过该字段");
                return null;
            }
            String[] parts = sortingField.split(":");
            if (parts.length != 2) {
                log.warn("排序字段格式错误：{}，正确格式为 'field:asc' 或 'field:desc'", sortingField);
                return null;
            }
            String field = parts[0].trim();
            String order = parts[1].trim();
            if (StrUtil.isBlank(field)) {
                log.warn("字段名为空，跳过该排序字段：{}", sortingField);
                return null;
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setColumn(field);
            orderItem.setAsc(SortableParam.SortingField.ORDER_ASC.equalsIgnoreCase(order));
            log.debug("构建排序项：字段[{}]，排序[{}]", field, order);
            return orderItem;
        });

    }
}

