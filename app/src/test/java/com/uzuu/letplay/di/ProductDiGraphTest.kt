package com.uzuu.letplay.di

import com.uzuu.letplay.MainApplication
import com.uzuu.letplay.data.repository.ProductRepositoryImpl
import com.uzuu.letplay.domain.repository.ProductRepository
import dagger.hilt.android.EntryPointAccessors
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = MainApplication::class)
class ProductDiGraphTest {

    @Test
    fun testProductBindingsAreResolvedFromHiltGraph() {
        val app = RuntimeEnvironment.getApplication() as MainApplication
        val entryPoint = EntryPointAccessors.fromApplication(app, ProductEntryPoint::class.java)

        val repository = entryPoint.productRepository()
        assertNotNull(repository, "ProductRepository should be resolvable from Hilt graph")
        assertTrue(repository is ProductRepositoryImpl, "ProductRepository should be an instance of ProductRepositoryImpl")

        val repositoryImpl = entryPoint.productRepositoryImpl()
        assertNotNull(repositoryImpl, "ProductRepositoryImpl should be resolvable directly")
        assertSame(repository, repositoryImpl, "ProductRepository and ProductRepositoryImpl should be the same singleton instance")

        val api = entryPoint.practiceProductApi()
        assertNotNull(api, "PracticeProductApi should be resolvable from Hilt graph")

        val errorMapper = entryPoint.practiceDataErrorMapper()
        assertNotNull(errorMapper, "PracticeDataErrorMapper should be resolvable from Hilt graph")

        val getProductsUseCase = entryPoint.getProductsUseCase()
        assertNotNull(getProductsUseCase, "GetProductsUseCase should be resolvable from Hilt graph")

        val getProductDetailUseCase = entryPoint.getProductDetailUseCase()
        assertNotNull(getProductDetailUseCase, "GetProductDetailUseCase should be resolvable from Hilt graph")

        val getCategoriesUseCase = entryPoint.getCategoriesUseCase()
        assertNotNull(getCategoriesUseCase, "GetCategoriesUseCase should be resolvable from Hilt graph")
    }
}
