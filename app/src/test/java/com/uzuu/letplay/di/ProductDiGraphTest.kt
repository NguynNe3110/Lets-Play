package com.uzuu.letplay.di

import com.uzuu.letplay.MainApplication
import com.uzuu.letplay.domain.repository.ProductRepository
import dagger.hilt.android.EntryPointAccessors
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = MainApplication::class)
class ProductDiGraphTest {

    @Test
    fun testProductBindingsAreResolvedFromHiltGraph() {
        val app = RuntimeEnvironment.getApplication() as MainApplication
        val entryPoint = EntryPointAccessors.fromApplication(app, ProductEntryPoint::class.java)

        val repository = entryPoint.productRepository()
        assertNotNull(repository, "ProductRepository should be resolvable from Hilt graph")
        assertSame(repository, entryPoint.productRepository(), "Repository binding must retain its singleton scope")

        val getProductsUseCase = entryPoint.getProductsUseCase()
        assertNotNull(getProductsUseCase, "GetProductsUseCase should be resolvable from Hilt graph")

        val getProductDetailUseCase = entryPoint.getProductDetailUseCase()
        assertNotNull(getProductDetailUseCase, "GetProductDetailUseCase should be resolvable from Hilt graph")

        val getCategoriesUseCase = entryPoint.getCategoriesUseCase()
        assertNotNull(getCategoriesUseCase, "GetCategoriesUseCase should be resolvable from Hilt graph")
    }
}
