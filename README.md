# PLAYLIST for NontonTV

branch ini berisikan playlist untuk aplikasi NontonTV v2.x

## FORMAT

contoh json yg digunakan :
```json
{
  "categories": [
    {
      "name": "kategori 1",
      "channels": [
        {
          "name": "siaran 1",
          "stream_url": "https://...",
          "drm_name": "nama drm"
        },
        {
          "name": "siaran 2",
          "stream_url": "https://...",
          "drm_name": "nama drm"
        }
      ],
      "name": "kategori 2",
      "channels": [
        {
          "name": "siaran 3",
          "stream_url": "https://..."
        }
      ]
    }
  ],
  "drm_licenses": [
    {
      "drm_name": "nama drm",
      "drm_url": "https://..."
    }
  ]
}
```
*notes*: `drm_name` pada channel bisa dihilangkan kalau tdk pakai drm
