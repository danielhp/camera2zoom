
package com.sample.android.zoom.module

import com.sample.android.zoom.ui.CameraPreviewViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun provideCameraPreviewViewModel(
        @ApplicationScope scope: CoroutineScope,
    ): CameraPreviewViewModel {
        return CameraPreviewViewModel(scope)
    }
}