window.APP_CONFIG = {

    githubLink: "https://github.com/iqubb/cloud-storage",

    mainName: "CLOUD",

    baseUrl: "",

    baseApi: "/api",

    validateLoginForm: true,
    validateRegistrationForm: true,

    validUsername: {
        minLength: 3,
        maxLength: 20,
        pattern: "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$",
    },

    validPassword: {
        minLength: 6,
        maxLength: 20,
        pattern: "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>[\\]/`~+=-_';]*$",
    },

    validFolderName: {
        minLength: 1,
        maxLength: 200,
        pattern: "^[^/\\\\:*?\"<>|]+$",
    },

    isMoveAllowed: true,

    isCutPasteAllowed: true,

    isFileContextMenuAllowed: true,

    isShortcutsAllowed: true,

    functions: {
        mapObjectToFrontFormat: (obj) => {
            return {
                lastModified: null,
                name: obj.name,
                size: obj.size,
                path: obj.path + obj.name,
                folder: obj.type === "DIRECTORY"
            }
        },

    }
};