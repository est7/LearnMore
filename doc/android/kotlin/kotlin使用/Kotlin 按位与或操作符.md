**一、位操作：**

shl(bits) – 左移位 (Java’s <<)
shr(bits) – 右移位 (Java’s >>)
ushr(bits) – 无符号右移位 (Java’s >>>)
and(bits) – 与  &
or(bits) – 或  ||
xor(bits) – 异或
inv() – 反向

```Kotlin
    val a = 5    val b = a shl 2 //左移2位，5*2*2=20    println(b)  //20
```

 

**二、位运算符**:

| 运算符     | 表示含义         |
| :--------- | :--------------- |
| and(bits)  | 按位与           |
| or(bits)   | 按位或           |
| inv(bits)  | 按位非           |
| xor(bits)  | 按位异或         |
| shl(bits)  | 左移运算符       |
| shr(bits)  | 右移运算符       |
| ushr(bits) | 无符号右移运算符 |

**三、Kotlin的位运算符只能对Int和Long两种数据类型起作用。**

**四、位操作和位运算实例**

通过位运算来保证头尾不超过数组范围，通过位操作来扩容（数组长度保持为2的整数倍，方便进行位运算）

```Kotlin
//如ArrayDeque通过位与运算(等价于java中的'&')，保证头尾不超过数组边界
class SimpleIntArrayDeque {
    private var elements: Array<Int?> = arrayOfNulls(16) //扩容数组
    private var head: Int = 0 //头
    private var tail: Int = elements.size //尾，tail-1是当前最后一位数据
 
    fun addFirst(value: Int) {
        if (value == null)
            throw NullPointerException()
        //当head-1为-1时，实际上是11111111&00111111，结果是00111111，也就是物理数组的尾部15；
        head = (head - 1) and (elements.size - 1)
        elements[head] = value
        if (head == tail)
            doubleCapacity()
    }
 
    fun addLast(value: Int) {
        if (value == null)
            throw NullPointerException()
        elements[tail] = value
        //当tail+1为16时，实际上是01000000&00111111，结果是00000000，也就是物理数组的头部0；
        tail = (tail + 1) and (elements.size - 1)
        if (tail == head)
            doubleCapacity()
    }
    
    fun pollFirst(): Int? {
        val h = head
        val result = elements[h]
        if (result != null) {
            elements[h] = null
            head = (h + 1) and (elements.size - 1)
        }
        return result
    }
    
    fun pollLast(): Int? {
        val t = (tail - 1) and (elements.size - 1)
        val result = elements[t]
        if (result != null) {
            elements[t] = null
            tail = t
        }
        return result
    }
 
    //扩容：插入数据前，判断插入后将头尾相等（即插入后数组将填满），则立即扩容
    private fun doubleCapacity() {
        val p = head
        val n = elements.size
        val r = n - p
        var newCapacity = n shl 1  //扩容2倍
        if (newCapacity < 0)
            throw IllegalArgumentException("Sorry, deque too big")
        var newElements: Array<Int?> = arrayOfNulls(newCapacity)
        //从头部开始拷贝,拷贝头部以后的所有内容，并把头部位置重置为0
        System.arraycopy(elements, p, newElements, 0, r)
        /**
         * 从0开始拷贝，拷贝头部之前的内容，并把拷贝内容接上刚才拷贝的位置，
         * 使得原来的数组放到新数组的前半部分
         */
        System.arraycopy(elements, 0, newElements, r, p)
        //释放旧的数组内存
        Arrays.fill(elements, null)
        elements = newElements
        head = 0
        tail = n
    }
    
    fun size(): Int {  //插入前判断，若插入后占满则立即扩容，因此size不会大于数组长度减一
        return (tail - head) and (elements.size - 1)
    }
    
    fun isEmpty(): Boolean {
        return head == tail
    }
 
    fun peekFirst(): Int? {
        return elements[head]
    }
 
    fun peekLast(): Int? {
        return elements[(tail -1) and (elements.size - 1)]
    }
}
```

 