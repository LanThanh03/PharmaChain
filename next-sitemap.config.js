/** @type {import('next-sitemap').IConfig} */
module.exports = {
  siteUrl: process.env.NEXT_PUBLIC_APP_PROTOCOL + '://' + process.env.NEXT_PUBLIC_APP_HOST + (process.env.NEXT_PUBLIC_APP_PORT ? ':' + process.env.NEXT_PUBLIC_APP_PORT : ''),
  generateRobotsTxt: true,
  // Skip additional paths to avoid API calls during build in development
  additionalPaths: async (config) => {
    console.log('✅ [next-sitemap] Skipping dynamic content generation (development mode)');
    return [];
  },
}

