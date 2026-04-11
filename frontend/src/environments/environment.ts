export const environment = {
    production: false,
    apiUrl: 'http://localhost:8080/api',

    // Optional configurations
    enableDebugMode: true,
    enableConsoleLogging: true,

    // Feature flags
    features: {
        qrScanner: true,
        aiQualityCheck: true,
        notifications: true
    }
};
