package com.example.aspectj;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (C) 2013, Xiaomi Inc. All rights reserved.
 */

public class Test {
    public static void main(String[] a){
        // 第三个参数用于指定accessOrder值
//        LinkedHashMap<Integer, Entry> linkedHashMap = new LinkedHashMap<>(0, 0.75f, true );
//        linkedHashMap.put(3, new Entry("josan3"));
//        linkedHashMap.put(1, new Entry("josan1"));
//        linkedHashMap.put(2, new Entry("josan2"));
//
//        System.out.println("开始时顺序：");
//        Set<Map.Entry<Integer, Entry>> set = linkedHashMap.entrySet();
//        Iterator<Map.Entry<Integer, Entry>> iterator = set.iterator();
//        while(iterator.hasNext()) {
//            Map.Entry entry = iterator.next();
//            int key = (int) entry.getKey();
//            System.out.println("key:" + key );
//        }
//        System.out.println("通过get方法，导致key为name1对应的Entry到表尾");
//        linkedHashMap.get("name1");
//        Set<Map.Entry<String, String>> set2 = linkedHashMap.entrySet();
//        Iterator<Map.Entry<String, String>> iterator2 = set2.iterator();
//        while(iterator2.hasNext()) {
//            Map.Entry entry = iterator2.next();
//            String key = (String) entry.getKey();
//            String value = (String) entry.getValue();
//            System.out.println("key:" + key + ",value:" + value);
//        }
    }


    public static final class Entry {
        public  String value;

        /**
         * Lengths of this entry's files.
         */
        private  long[] lengths;

        /**
         * True if this entry has ever been published
         */
        private int readable;

        private Entry(String key) {
            this.value = key;
        }
    }
}
