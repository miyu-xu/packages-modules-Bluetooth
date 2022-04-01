/******************************************************************************
 *
 *  Copyright 2022 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include <base/strings/string_number_conversions.h>
#include <dirent.h>
#include <sys/stat.h>

#include "bta_gattc_int.h"

using gatt::StoredAttribute;

#define GATT_CACHE_PREFIX "/data/misc/bluetooth/gatt_cache_"
#define GATT_CACHE_VERSION 6

#define GATT_HASH_MAX_SIZE 30
#define GATT_HASH_PATH_PREFIX "/data/misc/bluetooth/gatt_hash_"
#define GATT_HASH_PATH "/data/misc/bluetooth"
#define GATT_HASH_FILE_PREFIX "gatt_hash_"

static void bta_gattc_hash_remove_least_recently_used_if_possible();

static void bta_gattc_generate_cache_file_name(char* buffer, size_t buffer_len,
                                               const RawAddress& bda) {
  snprintf(buffer, buffer_len, "%s%02x%02x%02x%02x%02x%02x", GATT_CACHE_PREFIX,
           bda.address[0], bda.address[1], bda.address[2], bda.address[3],
           bda.address[4], bda.address[5]);
}

static void bta_gattc_generate_hash_file_name(char* buffer, size_t buffer_len,
                                              const Octet16& hash) {
  snprintf(buffer, buffer_len, "%s%s", GATT_HASH_PATH_PREFIX,
           base::HexEncode(hash.data(), 16).c_str());
}

static gatt::Database EMPTY_DB;

/*******************************************************************************
 *
 * Function         bta_gattc_load_db
 *
 * Description      Load GATT database from storage.
 *
 * Parameter        fname: input file name
 *
 * Returns          non-empty GATT database on success, empty GATT database
 *                  otherwise
 *
 ******************************************************************************/
static gatt::Database bta_gattc_load_db(const char* fname) {
  FILE* fd = fopen(fname, "rb");
  if (!fd) {
    LOG(ERROR) << __func__ << ": can't open GATT cache file " << fname
               << " for reading, error: " << strerror(errno);
    return EMPTY_DB;
  }

  uint16_t cache_ver = 0;
  uint16_t num_attr = 0;

  if (fread(&cache_ver, sizeof(uint16_t), 1, fd) != 1) {
    LOG(ERROR) << __func__ << ": can't read GATT cache version from: " << fname;
    goto done;
  }

  if (cache_ver != GATT_CACHE_VERSION) {
    LOG(ERROR) << __func__ << ": wrong GATT cache version: " << fname;
    goto done;
  }

  if (fread(&num_attr, sizeof(uint16_t), 1, fd) != 1) {
    LOG(ERROR) << __func__
               << ": can't read number of GATT attributes: " << fname;
    goto done;
  }

  {
    std::vector<StoredAttribute> attr(num_attr);

    if (fread(attr.data(), sizeof(StoredAttribute), num_attr, fd) != num_attr) {
      LOG(ERROR) << __func__ << ": can't read GATT attributes: " << fname;
      goto done;
    }

    bool success = false;
    gatt::Database result = gatt::Database::Deserialize(attr, &success);
    return success ? result : EMPTY_DB;
  }

done:
  fclose(fd);
  return EMPTY_DB;
}

/*******************************************************************************
 *
 * Function         bta_gattc_cache_load
 *
 * Description      Load GATT cache from storage for server.
 *
 * Parameter        bd_address: remote device address
 *
 * Returns          non-empty GATT database on success, empty GATT database
 *                  otherwise
 *
 ******************************************************************************/
gatt::Database bta_gattc_cache_load(const RawAddress& server_bda) {
  char fname[255] = {0};
  bta_gattc_generate_cache_file_name(fname, sizeof(fname), server_bda);
  return bta_gattc_load_db(fname);
}


/*******************************************************************************
 *
 * Function         bta_gattc_hash_load
 *
 * Description      Load GATT cache from storage for server.
 *
 * Parameter        hash: 16-byte value
 *
 * Returns          non-empty GATT database on success, empty GATT database
 *                  otherwise
 *
 ******************************************************************************/
gatt::Database  bta_gattc_hash_load(const Octet16& hash) {
  char fname[255] = {0};
  bta_gattc_generate_hash_file_name(fname, sizeof(fname), hash);
  return bta_gattc_load_db(fname);
}

/*******************************************************************************
 *
 * Function         bta_gattc_store_db
 *
 * Description      Storess GATT db.
 *
 * Parameter        fname: output file name
 *                  attr: attributes to save.
 *
 * Returns          true on success, false otherwise
 *
 ******************************************************************************/
static bool bta_gattc_store_db(const char* fname,
                               const std::vector<StoredAttribute>& attr) {
  FILE* fd = fopen(fname, "wb");
  if (!fd) {
    LOG(ERROR) << __func__
               << ": can't open GATT cache file for writing: " << fname;
    return false;
  }

  uint16_t cache_ver = GATT_CACHE_VERSION;
  if (fwrite(&cache_ver, sizeof(uint16_t), 1, fd) != 1) {
    LOG(ERROR) << __func__ << ": can't write GATT cache version: " << fname;
    fclose(fd);
    return false;
  }

  uint16_t num_attr = attr.size();
  if (fwrite(&num_attr, sizeof(uint16_t), 1, fd) != 1) {
    LOG(ERROR) << __func__
               << ": can't write GATT cache attribute count: " << fname;
    fclose(fd);
    return false;
  }

  if (fwrite(attr.data(), sizeof(StoredAttribute), num_attr, fd) != num_attr) {
    LOG(ERROR) << __func__ << ": can't write GATT cache attributes: " << fname;
    fclose(fd);
    return false;
  }

  fclose(fd);
  return true;
}

/*******************************************************************************
 *
 * Function         bta_gattc_cache_write
 *
 * Description      This callout function is executed by GATT when a server
 *                  cache is available to save.
 *
 * Parameter        server_bda: server bd address of this cache belongs to
 *                  database: attributes to save.
 * Returns
 *
 ******************************************************************************/
void bta_gattc_cache_write(const RawAddress& server_bda,
                           const gatt::Database& database) {
  char fname[255] = {0};
  bta_gattc_generate_cache_file_name(fname, sizeof(fname), server_bda);
  bta_gattc_store_db(fname, database.Serialize());
}

/*******************************************************************************
 *
 * Function         bta_gattc_cache_link
 *
 * Description      Link address-database file to hash-database file
 *
 * Parameter        server_bda: server bd address of this cache belongs to
 *                  hash: 16-byte value
 *
 * Returns          true on success, false otherwise
 *
 ******************************************************************************/
void bta_gattc_cache_link(const RawAddress& server_bda, const Octet16& hash) {
  char addr_file[255] = {0};
  char hash_file[255] = {0};
  bta_gattc_generate_cache_file_name(addr_file, sizeof(addr_file), server_bda);
  bta_gattc_generate_hash_file_name(hash_file, sizeof(hash_file), hash);

  unlink(addr_file);  // remove addr file first if the file exists
  int result = link(hash_file, addr_file);

  LOG(INFO) << __func__ << ": link " << addr_file << " to " << hash_file
            << ", result=" << result;
}

/*******************************************************************************
 *
 * Function         bta_gattc_hash_write
 *
 * Description      This callout function is executed by GATT when a server
 *                  cache is available to save for specific hash.
 *
 * Parameter        hash: 16-byte value
 *                  database: gatt::Database instance.
 *
 * Returns          true on success, false otherwise
 *
 ******************************************************************************/
bool bta_gattc_hash_write(const Octet16& hash,
                          const gatt::Database& database) {
  char fname[255] = {0};
  bta_gattc_generate_hash_file_name(fname, sizeof(fname), hash);
  bta_gattc_hash_remove_least_recently_used_if_possible();
  return bta_gattc_store_db(fname, database.Serialize());
}

/*******************************************************************************
 *
 * Function         bta_gattc_cache_reset
 *
 * Description      This callout function is executed by GATTC to reset cache in
 *                  application
 *
 * Parameter        server_bda: server bd address of this cache belongs to
 *
 * Returns          void.
 *
 ******************************************************************************/
void bta_gattc_cache_reset(const RawAddress& server_bda) {
  VLOG(1) << __func__;
  char fname[255] = {0};
  bta_gattc_generate_cache_file_name(fname, sizeof(fname), server_bda);
  unlink(fname);
}

/*******************************************************************************
 *
 * Function         bta_gattc_hash_remove_least_recently_used_if_possible
 *
 * Description      When the max size reaches, find the oldest item and remove
 *                  it if possible
 *
 * Parameter
 *
 * Returns          void
 *
 ******************************************************************************/
static void bta_gattc_hash_remove_least_recently_used_if_possible() {
  std::unique_ptr<DIR, decltype(&closedir)> dirp(opendir(GATT_HASH_PATH),
                                                 &closedir);
  if (dirp == nullptr) {
    LOG(ERROR) << __func__ << ": open dir error, " << GATT_HASH_PATH;
    return;
  }

  time_t lru_time = time(NULL);
  size_t count = 0;
  char to_be_removed[255] = {0};

#if (BTA_GATT_DEBUG == TRUE)
  LOG(INFO) << "<-----------Start Local Hash Cache---------->";
#endif

  dirent* dp;
  while ((dp = readdir(dirp.get())) != nullptr) {
    if (strcmp(".", dp->d_name) == 0 || strcmp("..", dp->d_name) == 0) {
      continue;
    }

    // pattern match: gatt_hash_
    size_t fname_len = strlen(dp->d_name);
    size_t pattern_len = strlen(GATT_HASH_FILE_PREFIX);
    if (pattern_len > fname_len) {
      continue;
    }

    char tmp[255] = {0};
    strncpy(tmp, dp->d_name, pattern_len);
    if (strcmp(tmp, GATT_HASH_FILE_PREFIX) != 0) {
      continue;
    }

    // increase hash file count
    count++;

    // check hard link count of the file
    struct stat buf;
    snprintf(tmp, 255, "%s/%s", GATT_HASH_PATH, dp->d_name);

#if (BTA_GATT_DEBUG == TRUE)
    int result = lstat(tmp, &buf);
    LOG(INFO) << "name=" << dp->d_name << ", result=" << result
              << ", linknum=" << buf.st_nlink << ", mtime=" << buf.st_mtime;
#else
    lstat(tmp, &buf);
#endif

    // if hard link count of the file is 1, it means no trusted device links to
    // the inode. It is safe to be a candidate to be removed
    if (buf.st_nlink == 1 && buf.st_mtime < lru_time) {
      lru_time = buf.st_mtime;
      snprintf(to_be_removed, 255, "%s/%s", GATT_HASH_PATH, dp->d_name);
    }
  }

#if (BTA_GATT_DEBUG == TRUE)
  LOG(INFO) << "<-----------End Local Hash Cache------------>";
#endif

  // if the number of hash files exceeds the limit, remove the oldest item.
  // There is one exception, that is, if all hash files are linked by trusted
  // devices, then nothing can be removed
  if (count > GATT_HASH_MAX_SIZE && strcmp(to_be_removed, "") != 0) {
    unlink(to_be_removed);
    LOG(INFO) << __func__ << ": remove " << to_be_removed;
  }
}
