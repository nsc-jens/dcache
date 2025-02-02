THE CLEANER SERVICE
==================================

The cleaner service is a two-cell component consisting of `cleaner-disk` and `cleaner-hsm` that watches for files being deleted in the namespace. When files are deleted, the `cleaner-disk` cell will notify the pools that hold a copy of the deleted files' data and tell the pools to remove that data. Optionally, the `cleaner-hsm`, when present, can instruct HSM-attached pools to remove copies of the file's data stored on tape.

The cleaner components run periodically, so there may be a delay between a file being deleted in the namespace and the corresponding deletion of that file's data in pools and on tape.

-----
[TOC bullet hierarchy]
-----

## Configuration

The cleaner services can be run in high availability (HA) mode (coordinated via [ZooKeeper](config-zookeeper.md)) by having several `cleaner-disk` and `cleaner-hsm` cells in a dCache instance, which need to share the same database. Only one such cleaner instance per type (disk or hsm) will be active at any point in time. If the currently active one ("master") is unavailable, another one is automatically taking its place.

### Disk Cleaner

The `cleaner-disk` is responsible for removing disk-resident file replicas.

The property `cleaner-disk.limits.threads` controls the number of pools processed in parallel.
The `cleaner-disk.limits.batch-size` property places an upper limit on the number of files' data to be deleted in a message. If more than this number of files are to be deleted then the pool will receive multiple messages.

The `cleaner-disk` maintains a list of pools that it was unable to contact: pools are either offline or sufficiently overloaded that they couldn't respond in time. A `cleaner-disk` cell will periodically try to delete data on pools in this list, but between such retries these pools are excluded from cleaner activity. This period can be configured via `cleaner-disk.service.pool.retry-period`.

### HSM Cleaner

The `cleaner-hsm` is responsible for removing tape-resident file replicas by instructing HSM-attached pools to remove deleted files' data stored in the HSM. To enable this feature, the property must be enabled at all the pools that are supposed to delete files from an HSM.

The `cleaner-hsm` component runs sequentially by sending `cleaner-hsm.limits.batch-size` delete targets in a message to an hsm-connected pool. It does so per hsm.

Regularly, it fetches hsm locations of files to delete from the database and caches them for clustering by hsm. In order to not overload the memory of a `cleaner-hsm` cell, the number of hsm delete locations that are cached at any point in time should be limited. The `cleaner-hsm.limits.max-cached-locations = 12000` allows to set such a limit.

When a pool reports back to `cleaner-hsm` that it was unable to delete certain files, `cleaner-hsm` removes these entries from the local cache. They will be re-loaded into the locations cache eventually so that they can be retried.
