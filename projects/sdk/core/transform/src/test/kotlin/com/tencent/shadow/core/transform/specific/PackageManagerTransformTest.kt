package com.tencent.shadow.core.transform.specific

import com.tencent.shadow.core.transform.common.ShadowTransformTest
import javassist.CtClass
import javassist.CtMethod
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageManagerTransformTest : ShadowTransformTest() {

    val packageManagerClazz = sLoader["android.content.pm.PackageManager"]

    @Test
    fun testPackageManagerTransform() {
        val allInputClass = setOf(sLoader["test.TestPackageManager"], sLoader["test.TestPackageManager$1"], sLoader["test.TestPackageManager$1$1"])

        val packageManagerTransform = PackageManagerTransform()
        packageManagerTransform.mClassPool = sLoader
        packageManagerTransform.setup(allInputClass)

        val methods = arrayOf("getApplicationInfo","getActivityInfo")


        allInputClass.forEach {
            for (method in methods) {
                beforeTransformCheck(it, method)
            }
        }



        packageManagerTransform.list.forEach { transform ->
            transform.filter(allInputClass).forEach {
                it.defrost()
                transform.transform(it)
                it.writeFile(WRITE_FILE_DIR)
            }
        }



        allInputClass.forEach {
            for (method in methods) {
                afterTransformCheck(it, method)
            }
        }


    }


    fun beforeTransformCheck(clazz: CtClass, method: String) {
        val getApplicationMethods = packageManagerClazz.getDeclaredMethods(method)

        assertTrue("transform 前应该可以找到PackageManager的" + method + "的调用",
                findCall(getApplicationMethods, clazz)
        )
    }


    fun afterTransformCheck(clazz: CtClass, method: String) {
        val getManagerMethods = packageManagerClazz.getDeclaredMethods(method)

        assertTrue("transform 后应该可以不能找到PackageManager的" + method + "的调用",
                !findCall(getManagerMethods, clazz)
        )


        val methods2: List<CtMethod> = getTargetMethods(sLoader, arrayOf(clazz.name), arrayOf(method + "_shadow"))

        assertTrue(method + "_shadow方法应该能找到,且应该只有一个",
                methods2.size == 1
        )

        assertTrue(method + "_shadow方法调用也应该可以找到",
                findCall(arrayOf(clazz.getDeclaredMethod(method + "_shadow")), clazz)
        )
    }

    fun findCall(target: Array<CtMethod>, clazz: CtClass): Boolean {
        clazz.defrost()
        var isFind = false
        for (methods in target) {
            if (matchMethodCallInClass(methods, clazz)) {
                isFind = true
                break
            }
        }
        return isFind
    }


}