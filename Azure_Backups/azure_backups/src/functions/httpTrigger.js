const { app } = require('@azure/functions');

app.http('httpTrigger', {
    methods: ['GET', 'POST'],
    authLevel: 'anonymous',
    handler: async (request, context) => {
        context.log(`Procesando solicitud HTTP para URL "${request.url}"`);

        return {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                message: '¡Tu función de Azure en Node.js (v4) está ejecutándose correctamente!',
                timestamp: new Date().toISOString()
            })
        };
    }
});
