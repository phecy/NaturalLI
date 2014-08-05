#ifndef UTILS_H
#define UTILS_H

#include <ctime>
#include <vector>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <execinfo.h>
#include <cxxabi.h>
#include <assert.h>

#include <config.h>
#include "Types.h"
#include "Graph.h"
#include "Search.h"

/**
 * Print the string gloss for the given fact.
 */
std::string toString(const Graph& graph, const tagged_word* fact, const uint8_t factSize);

/**
 * Print a human readable dump of a search path.
 */
std::string toString(const Graph& graph, SearchType& searchType, const Path* path);

/**
 * Print out the edge type being taken.
 */
std::string toString(const edge_type& edge);

/**
 * Print out elapsed time in days+hours+minutes+seconds.
 */
std::string toString(const time_t& elapsedTime);

/**
 *  The fact (lemur, have, tail)
 */
const std::vector<tagged_word> lemursHaveTails();

/**
 *  The fact (animal, have, tail)
 */
const std::vector<tagged_word> animalsHaveTails();

/**
 *  The fact (cat, have, tail)
 */
const std::vector<tagged_word> catsHaveTails();

/**
 *  The fact (some, dogs, chase, cats)
 */
const std::vector<tagged_word> someDogsChaseCats();

/**
 * stacktrace.h (c) 2008, Timo Bingmann from http://idlebox.net/
 * published under the WTFPL v2.0
 */
void print_stacktrace(FILE *out = stderr, unsigned int max_frames = 63);

/**
 * Convert an edge type to the MacCartney style projection function.
 */
inference_function edge2function(const edge_type& type);

/**
 * atoi() is actually quite slow, and we don't need all its special cases.
 */
inline uint32_t fast_atoi( const char * str ) {
  assert (str[0] > '0' || str[1] == '\0');  // note: no leading zeroes
  uint32_t val = 0;
  char c = *str;
  while( c != '\0' ) {
    assert (str[0] >= '0');
    assert (str[0] <= '9');
    val = val*10 + (c - '0');
    str += 1;
    c = *str;
    if (c == '}') { return val; }
  }
  return val;
}

#endif
