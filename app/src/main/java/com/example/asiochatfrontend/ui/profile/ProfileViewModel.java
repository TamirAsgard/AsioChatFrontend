package com.example.asiochatfrontend.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.domain.usecase.user.GetUserByIdUseCase;

import javax.inject.Inject;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<UserDto> profile = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(ConnectionManager connectionManager) {
        try {
            GetUserByIdUseCase useCase = new GetUserByIdUseCase(connectionManager);
            profile.setValue(useCase.execute("current_user_id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LiveData<UserDto> getProfile() {
        return profile;
    }
}
