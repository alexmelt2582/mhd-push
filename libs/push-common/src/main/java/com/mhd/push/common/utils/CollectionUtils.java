package com.mhd.push.common.utils;

import cn.hutool.core.collection.CollUtil;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zhao-hao-dong
 **/
public class CollectionUtils {
    private CollectionUtils() {
    }

    /**
     * 对比老、新两个列表，找出新增、未修改、删除的数据
     *
     * @param oldList  老列表
     * @param newList  新列表
     * @param sameFunc 对比函数，返回 true 表示相同，返回 false 表示不同
     *                 注意，same 是通过每个元素的“标识”，判断它们是不是同一个数据
     * @return [新增列表、未修改列表、删除列表]
     */
    public static <T> List<List<T>> diffList(Collection<T> oldList, Collection<T> newList,
                                             BiFunction<T, T, Boolean> sameFunc) {
        if (CollUtil.isEmpty(oldList) && CollUtil.isEmpty(newList)) {
            return Arrays.asList(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        } else if (CollUtil.isEmpty(oldList)) {
            // 旧列表为空，则新增列表为新列表，删除列表为空
            return Arrays.asList(new ArrayList<>(newList), Collections.emptyList(), Collections.emptyList());
        } else if (CollUtil.isEmpty(newList)) {
            // 新列表为空，则新增列表为空，删除列表为旧列表
            return Arrays.asList(Collections.emptyList(), Collections.emptyList(), new ArrayList<>(oldList));
        }
        List<T> createList = new LinkedList<>(newList);
        List<T> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>();
        // 通过以 oldList 为主遍历，找出 updateList 和 deleteList
        for (T oldObj : oldList) {
            // 1. 寻找是否有匹配的
            T foundObj = null;
            for (Iterator<T> iterator = createList.iterator(); iterator.hasNext(); ) {
                T newObj = iterator.next();
                // 1.1 不匹配，则直接跳过
                if (!sameFunc.apply(oldObj, newObj)) {
                    continue;
                }
                // 1.2 匹配，则移除，并结束寻找
                iterator.remove();
                foundObj = newObj;
                break;
            }
            // 2. 匹配添加到 updateList；不匹配则添加到 deleteList 中
            if (foundObj != null) {
                updateList.add(foundObj);
            } else {
                deleteList.add(oldObj);
            }
        }
        return Arrays.asList(createList, updateList, deleteList);
    }

    public static <T> List<T> filterList(Collection<T> from, Predicate<T> predicate) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T, U> List<U> convertList(Collection<T> from, Function<T, U> func) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().map(func).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static <T, U> Set<U> convertSet(Collection<T> from, Function<T, U> func) {
        if (CollUtil.isEmpty(from)) {
            return new HashSet<>();
        }
        return from.stream().map(func).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * 把集合转成 Map，支持分别指定“取 key”和“取 value”的逻辑
     *
     * @param list        源集合
     * @param keyMapper   如何取 key
     * @param valueMapper 如何取 value
     * @param <T>         源类型
     * @param <K>         key 类型
     * @param <V>         value 类型
     * @return LinkedHashMap（保序）
     */
    public static <T, K, V> Map<K, V> convertMap(
            Collection<T> list,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyMap();
        }
        // 重复 key 取后者
        return list.stream()
                .collect(Collectors.toMap(
                        keyMapper,
                        valueMapper,
                        (v1, v2) -> v2));  // 保序
    }
}
