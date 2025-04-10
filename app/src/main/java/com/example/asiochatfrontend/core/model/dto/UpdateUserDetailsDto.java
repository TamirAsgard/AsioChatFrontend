package com.example.asiochatfrontend.core.model.dto;

public class UpdateUserDetailsDto {
    public UserDetailsDto userDetailsDto;

    public UpdateUserDetailsDto(UserDetailsDto userDetailsDto) {
        this.userDetailsDto = userDetailsDto;
    }

    public UserDetailsDto getUserDetailsDto() {
        return userDetailsDto;
    }

    public void setUserDetailsDto(UserDetailsDto userDetailsDto) {
        this.userDetailsDto = userDetailsDto;
    }
}
