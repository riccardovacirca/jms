package com.example.auth.dto;

public record TwoFactorCredentialDTO(String challengeToken, String pin) {}
