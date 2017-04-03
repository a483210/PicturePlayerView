package com.xiuyukeji.pictureplayerview;

import com.xiuyukeji.pictureplayerview.utils.CacheList;
import com.xiuyukeji.pictureplayerview.utils.CacheList.OnRemoveListener;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * 测试CacheList
 *
 * @author Created by jz on 2017/4/1 16:17
 */
public class CacheListTestCase {

    private static final int DEFAULT_MAX_COUNT = 12;

    @Test
    public void testAdd() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        addValueCount(cacheList, 2);

        assertEquals(cacheList.size(), 2);
    }

    @Test
    public void testGetFirst() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertNull(cacheList.getFirst());

        addValueCount(cacheList, 1);

        assertNotNull(cacheList.getFirst());
    }

    @Test
    public void testGet() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertNull(cacheList.get(0));

        addValueCount(cacheList, 1);

        assertEquals(cacheList.size(), 1);

        assertNotNull(cacheList.get(0));

        addValueCount(cacheList, 1, true);

        assertEquals(cacheList.get(1).tag, 1);
    }

    @Test
    public void testRemoveFirst() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 2);

        assertEquals(cacheList.size(), 2);

        cacheList.removeFirst();

        assertEquals(cacheList.size(), 1);

        cacheList.removeFirst();

        assertEquals(cacheList.size(), 0);
    }

    @Test
    public void testRemove() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 2);

        assertEquals(cacheList.size(), 2);

        cacheList.removeFirst();

        assertEquals(cacheList.size(), 1);

        cacheList.removeFirst();

        assertEquals(cacheList.size(), 0);
    }


    @Test
    public void testIsEmpty() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        addValueCount(cacheList, 1);

        assertFalse(cacheList.isEmpty());

        cacheList.removeFirst();

        assertTrue(cacheList.isEmpty());
    }

    @Test
    public void testRemove_ExceedMaxCount() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 6);

        assertEquals(cacheList.size(), 6);

        for (int i = 0; i < 8; i++) {
            cacheList.removeFirst();
        }

        assertEquals(cacheList.size(), 0);
    }

    @Test
    public void testRemoveCount() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 3);

        assertEquals(cacheList.size(), 3);

        cacheList.removeCount(2);

        assertEquals(cacheList.size(), 1);

        cacheList.removeCount(1);

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 5, true);

        cacheList.removeCount(4);

        assertEquals(cacheList.size(), 1);

        assertEquals(cacheList.getFirst().tag, 5);
    }

    @Test
    public void testRemoveCount_ExceedMaxCount() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 6);

        assertEquals(cacheList.size(), 6);

        cacheList.removeCount(8);

        assertEquals(cacheList.size(), 0);
    }

    @Test
    public void testClear() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        assertEquals(cacheList.size(), 0);

        addValueCount(cacheList, 2);

        assertEquals(cacheList.size(), 2);

        cacheList.clear();

        assertEquals(cacheList.size(), 0);
    }

    @Test
    public void testOnRemoveListener() throws Exception {
        TestOnRemoveListener onRemoveListener = new TestOnRemoveListener();
        CacheList<TestObject> cacheList = getTestData(onRemoveListener);

        addValueCount(cacheList, 5);

        assertEquals(onRemoveListener.removeCount, 0);

        cacheList.removeFirst();

        assertEquals(onRemoveListener.removeCount, 1);

        cacheList.removeCount(2);

        assertEquals(onRemoveListener.removeCount, 3);

        cacheList.clear();

        assertEquals(onRemoveListener.removeCount, 5);
    }

    @Test
    public void testAddExceedMaxCount_OnRemoveListener() throws Exception {
        TestOnRemoveListener onRemoveListener = new TestOnRemoveListener();
        CacheList<TestObject> cacheList = getTestData(onRemoveListener);

        assertEquals(onRemoveListener.removeCount, 0);

        addValueCount(cacheList, DEFAULT_MAX_COUNT);

        assertEquals(onRemoveListener.removeCount, 0);

        addValueCount(cacheList, 1);

        assertEquals(onRemoveListener.removeCount, 1);

        addValueCount(cacheList, DEFAULT_MAX_COUNT);

        assertEquals(onRemoveListener.removeCount, 1 + DEFAULT_MAX_COUNT);
    }

    @Test
    public void testGetIsCorrect() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        cacheList.add(new TestObject(1));

        assertEquals(cacheList.getFirst().tag, 1);

        cacheList.add(new TestObject(2));

        assertEquals(cacheList.getFirst().tag, 1);
    }

    @Test
    public void testGetIsCorrect_Removed() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        cacheList.add(new TestObject(1));

        assertEquals(cacheList.getFirst().tag, 1);

        cacheList.add(new TestObject(2));
        cacheList.removeFirst();

        assertEquals(cacheList.getFirst().tag, 2);
    }

    @Test
    public void testGetIsCorrect_AddExceedMaxCountAndRemoved() throws Exception {
        CacheList<TestObject> cacheList = getTestData();

        addValueCount(cacheList, DEFAULT_MAX_COUNT + 5, true);

        assertEquals(cacheList.size(), DEFAULT_MAX_COUNT);

        cacheList.removeCount(DEFAULT_MAX_COUNT - 1);

        assertEquals(cacheList.size(), 1);

        assertEquals(cacheList.getFirst().tag, DEFAULT_MAX_COUNT + 5);
    }

    @Test
    public void testMultiThread_AddAndRemove() throws Exception {
        final CacheList<TestObject> cacheList = getTestData(12000);

        addValueCount(cacheList, 6000);

        assertEquals(cacheList.size(), 6000);

        Thread addThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 4000; i++) {
                    cacheList.add(new TestObject());
                    cacheList.removeFirst();
                }
            }
        });
        addThread.start();
        Thread removeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 2000; i++) {
                    cacheList.removeFirst();
                    cacheList.add(new TestObject());
                }
            }
        });
        removeThread.start();

        addThread.join(5);
        removeThread.join(5);

        assertEquals(cacheList.size(), 6000);
    }

    private void addValueCount(CacheList<TestObject> cacheList, int count) {
        addValueCount(cacheList, count, false);
    }

    private void addValueCount(CacheList<TestObject> cacheList, int count, boolean isAssign) {
        for (int i = 0; i < count; i++) {
            TestObject object;
            if (isAssign) {
                object = new TestObject(i + 1);
            } else {
                object = new TestObject();
            }
            cacheList.add(object);
        }
    }

    private CacheList<TestObject> getTestData() {
        return getTestData(null);
    }

    private CacheList<TestObject> getTestData(OnRemoveListener<TestObject> l) {
        return getTestData(DEFAULT_MAX_COUNT, l);
    }

    private CacheList<TestObject> getTestData(int count) {
        return getTestData(count, null);
    }

    private CacheList<TestObject> getTestData(int count, OnRemoveListener<TestObject> l) {
        return new CacheList<>(new TestObject[count], l);
    }

    private static class TestObject {
        private int tag;

        private TestObject() {
        }

        private TestObject(int tag) {
            this.tag = tag;
        }
    }

    private static class TestOnRemoveListener implements OnRemoveListener<TestObject> {

        private int removeCount = 0;

        @Override
        public void onRemove(boolean isOverflow, TestObject value) {
            removeCount++;
        }
    }
}
